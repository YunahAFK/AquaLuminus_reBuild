package com.example.aqualuminus_rebuild.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.aqualuminus_rebuild.data.models.AquaLuminusDevice
import com.example.aqualuminus_rebuild.network.ESP32ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.toMutableList

class DeviceRepository private constructor(private val context: Context) {
    private val activityLogRepository = ActivityLogRepository()
    private val _devices = MutableStateFlow<List<AquaLuminusDevice>>(emptyList())
    private val apiClient = ESP32ApiClient.getInstance()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("aqualuminus_devices", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        loadDevicesFromStorage()
    }

    fun getAllDevices(): Flow<List<AquaLuminusDevice>> = _devices.asStateFlow()

    suspend fun addDevice(device: AquaLuminusDevice) {
        try {
            // verify device is reachable
            val deviceInfo = apiClient.getDeviceInfo(device.ipAddress, device.port)

            val verifiedDevice = device.copy(
                name = deviceInfo.device_name ?: device.name,
                version = deviceInfo.version ?: device.version,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )

            // add to list
            val currentDevices = _devices.value.toMutableList()
            val existingIndex = currentDevices.indexOfFirst { it.id == device.id }

            if (existingIndex >= 0) {
                currentDevices[existingIndex] = verifiedDevice
            } else {
                currentDevices.add(verifiedDevice)
            }

            _devices.value = currentDevices

            // save to persistent storage
            saveDevicesToStorage(currentDevices)

            Log.d("DeviceRepository", "device added and saved: ${device.name}")

        } catch (e: Exception) {
            Log.e("DeviceRepository", "failed to add device: ${device.name}", e)
            throw e
        }
    }

    suspend fun removeDevice(deviceId: String) {
        val currentDevices = _devices.value.filter { it.id != deviceId }
        _devices.value = currentDevices

        // save to persistent storage
        saveDevicesToStorage(currentDevices)

        Log.d("DeviceRepository", "device removed and saved: $deviceId")
    }

    suspend fun refreshDevices() {
        Log.d("DeviceRepository", "Refreshing all devices...")

        val currentDevices = _devices.value.toMutableList()
        val updatedDevices = currentDevices.map { device ->
            try {
                // Fetch both status and sensor data
                val status = apiClient.getDeviceStatus(device.ipAddress, device.port)
                val sensors = apiClient.getSensorData(device.ipAddress, device.port) // <-- ADD THIS LINE
                val currentTime = System.currentTimeMillis()

                if (!device.isOnline) {
                    activityLogRepository.addLogEntry(device.id, "CONNECTION", "${device.name} is now online.")
                }

                // handle UV state changes
                val updatedDevice = when {
                    status.uvLightOn && !device.isUVOn -> {
                        device.copy(
                            isOnline = true,
                            isUVOn = true,
                            uvStartTime = currentTime,
                            uvEndTime = null,
                            lastSeen = currentTime
                        )
                    }
                    !status.uvLightOn && device.isUVOn -> {
                        val sessionDuration = device.uvStartTime?.let { currentTime - it } ?: 0L
                        device.copy(
                            isOnline = true,
                            isUVOn = false,
                            uvEndTime = currentTime,
                            totalUVTime = device.totalUVTime + sessionDuration,
                            lastSeen = currentTime
                        )
                    }
                    else -> {
                        device.copy(
                            isOnline = true,
                            isUVOn = status.uvLightOn,
                            lastSeen = currentTime
                        )
                    }
                }

                // Copy sensor data to the updated device
                updatedDevice.copy( // <-- ADD THIS BLOCK
                    temperature = sensors.temperature_c,
                    ph = sensors.ph
                )

            } catch (e: Exception) {
                Log.w("DeviceRepository", "device ${device.name} appears offline", e)

                // Log when device goes offline after being online
                if (device.isOnline) {
                    activityLogRepository.addLogEntry(device.id, "CONNECTION", "${device.name} went offline.")
                }

                device.copy(isOnline = false)
            }
        }

        _devices.value = updatedDevices
        saveDevicesToStorage(updatedDevices)
    }

    // UV-control functions
    suspend fun turnUVOn(deviceId: String): Boolean {
        return try {
            val device = getDeviceById(deviceId) ?: return false
            apiClient.turnUVOn(device.ipAddress, device.port)
            activityLogRepository.addLogEntry(deviceId, "UV_STATUS", "UV light turned ON manually.") // <-- ADD THIS

            val currentTime = System.currentTimeMillis()
            updateDeviceUVStatus(deviceId, true, uvStartTime = currentTime)
            true
        } catch (e: Exception) {
            Log.e("DeviceRepository", "error turning UV ON for device: $deviceId", e)
            false
        }
    }

    suspend fun turnUVOff(deviceId: String): Boolean {
        return try {
            val device = getDeviceById(deviceId) ?: return false
            apiClient.turnUVOff(device.ipAddress, device.port)
            activityLogRepository.addLogEntry(deviceId, "UV_STATUS", "UV light turned OFF manually.") // <-- ADD THIS

            val currentTime = System.currentTimeMillis()
            val uvDuration = device.uvStartTime?.let { currentTime - it } ?: 0L
            updateDeviceUVStatus(
                deviceId = deviceId,
                isUVOn = false,
                uvEndTime = currentTime,
                additionalUVTime = uvDuration
            )
            true
        } catch (e: Exception) {
            Log.e("DeviceRepository", "error turning UV OFF for device: $deviceId", e)
            false
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ Persistence Functions --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    private fun saveDevicesToStorage(devices: List<AquaLuminusDevice>) {
        try {
            val json = gson.toJson(devices)
            sharedPreferences.edit()
                .putString(DEVICES_KEY, json)
                .apply()
            Log.d("DeviceRepository", "devices saved to storage: ${devices.size} devices")
        } catch (e: Exception) {
            Log.e("DeviceRepository", "failed to save devices to storage", e)
        }
    }

    private fun loadDevicesFromStorage() {
        try {
            val json = sharedPreferences.getString(DEVICES_KEY, null)
            if (json != null) {
                val type = object : TypeToken<List<AquaLuminusDevice>>() {}.type
                val savedDevices: List<AquaLuminusDevice> = gson.fromJson(json, type)
                _devices.value = savedDevices
                Log.d("DeviceRepository", "devices loaded from storage: ${savedDevices.size} devices")
            } else {
                Log.d("DeviceRepository", "no saved devices found")
            }
        } catch (e: Exception) {
            Log.e("DeviceRepository", "failed to load devices from storage", e)
            _devices.value = emptyList()
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ Persistence Functions --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ Helper Functions --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    fun getDevice(deviceId: String): AquaLuminusDevice? {
        return getDeviceById(deviceId)
    }

    private fun getDeviceById(deviceId: String): AquaLuminusDevice? {
        return _devices.value.find { it.id == deviceId }
    }

    private fun updateDeviceUVStatus(
        deviceId: String,
        isUVOn: Boolean,
        uvStartTime: Long? = null,
        uvEndTime: Long? = null,
        additionalUVTime: Long = 0L
    ) {
        val currentDevices = _devices.value.toMutableList()
        val deviceIndex = currentDevices.indexOfFirst { it.id == deviceId }

        if (deviceIndex >= 0) {
            val currentDevice = currentDevices[deviceIndex]

            currentDevices[deviceIndex] = currentDevice.copy(
                isUVOn = isUVOn,
                uvStartTime = uvStartTime ?: currentDevice.uvStartTime,
                uvEndTime = uvEndTime ?: currentDevice.uvEndTime,
                totalUVTime = currentDevice.totalUVTime + additionalUVTime,
                lastSeen = System.currentTimeMillis()
            )

            _devices.value = currentDevices
            saveDevicesToStorage(currentDevices)

            Log.d("DeviceRepository", "updated UV status for device $deviceId: UV=$isUVOn, " +
                    "StartTime=${uvStartTime ?: currentDevice.uvStartTime}, " +
                    "EndTime=${uvEndTime ?: currentDevice.uvEndTime}")
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ Helper Functions --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    companion object {
        @Volatile
        private var INSTANCE: DeviceRepository? = null
        private const val DEVICES_KEY = "saved_devices"

        fun getInstance(context: Context): DeviceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        // for backward compatibility, but requires context to be set elsewhere
        @Deprecated("Use getInstance(context) instead")
        fun getInstance(): DeviceRepository {
            return INSTANCE ?: throw IllegalStateException(
                "DeviceRepository not initialized. Call getInstance(context) first."
            )
        }
    }
}