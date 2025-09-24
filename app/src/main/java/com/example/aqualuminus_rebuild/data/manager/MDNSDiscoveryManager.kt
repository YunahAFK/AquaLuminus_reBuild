package com.example.aqualuminus_rebuild.data.manager

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.aqualuminus_rebuild.ui.screens.iot.DiscoveredDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MDNSDiscoveryManager(private val context: Context) {

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    companion object {
        private const val SERVICE_TYPE = "_http._tcp."
        private const val TAG = "MDNSDiscovery"
    }

    fun discoverDevices(): Flow<List<DiscoveredDevice>> = callbackFlow {
        val discoveredDevices = mutableMapOf<String, DiscoveredDevice>()

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started for: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}")

                // check if this might be an AquaLuminus device
                if (isAquaLuminusDevice(service)) {
                    resolveService(service) { resolvedService ->
                        val device = createDiscoveredDevice(resolvedService)
                        if (device != null) {
                            discoveredDevices[device.id] = device
                            trySend(discoveredDevices.values.toList())
                        }
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "service Lost: ${service.serviceName}")
                // remove by service name since we might not have the full device info
                val deviceToRemove = discoveredDevices.values.find {
                    it.name.contains(service.serviceName) || service.serviceName.contains(it.id)
                }
                deviceToRemove?.let {
                    discoveredDevices.remove(it.id)
                    trySend(discoveredDevices.values.toList())
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "discovery failed to start: error code: $errorCode")
                close(Exception("Discovery failed to start"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "discovery failed to stop: error code: $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "failed to start discovery", e)
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                Log.e(TAG, "error stopping discovery", e)
            }
        }
    }

    private fun isAquaLuminusDevice(service: NsdServiceInfo): Boolean {
        val serviceName = service.serviceName.lowercase()
        // devices advertise as "AquaLuminus_UV001" etc.
        return serviceName.contains("aqualuminus") ||
                serviceName.contains("uv") ||
                service.serviceType.contains("_http._tcp")
    }

    private fun resolveService(service: NsdServiceInfo, onResolved: (NsdServiceInfo) -> Unit) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "resolve failed for ${serviceInfo.serviceName}: Error code: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "service resolved: ${serviceInfo.serviceName}")
                Log.d(TAG, "Host: ${serviceInfo.host}")
                Log.d(TAG, "Port: ${serviceInfo.port}")
                onResolved(serviceInfo)
            }
        }

        try {
            nsdManager.resolveService(service, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "error resolving service", e)
        }
    }

    private fun createDiscoveredDevice(service: NsdServiceInfo): DiscoveredDevice? {
        return try {
            val host = service.host
            val port = service.port
            val serviceName = service.serviceName

            if (host == null) {
                Log.w(TAG, "Service host is null for $serviceName")
                return null
            }

            // extract device info from TXT records if available
            val txtRecords = parseTxtRecords(service)
            val deviceId = txtRecords["device_id"] ?: extractDeviceIdFromName(serviceName)
            val deviceName = txtRecords["device"] ?: "AquaLuminus"
            val version = txtRecords["version"] ?: "Unknown"

            DiscoveredDevice(
                id = deviceId,
                name = "$deviceName-$deviceId",
                ipAddress = host.hostAddress ?: host.hostName,
                macAddress = "Unknown", // will be fetched from device API
                deviceType = deviceName,
                signalStrength = calculateSignalStrength(), // estimated
                isAvailable = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "error creating discovered device", e)
            null
        }
    }

    private fun parseTxtRecords(service: NsdServiceInfo): Map<String, String> {
        val txtRecords = mutableMapOf<String, String>()
        try {
            service.attributes?.forEach { (key, value) ->
                if (key != null && value != null) {
                    txtRecords[key] = String(value, Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "error parsing TXT records", e)
        }
        return txtRecords
    }

    private fun extractDeviceIdFromName(serviceName: String): String {
        // extract device ID from service names like "AquaLuminus_UV001"
        return when {
            serviceName.contains("_") -> serviceName.substringAfterLast("_")
            serviceName.matches(".*UV\\d+.*".toRegex()) -> {
                val match = "UV\\d+".toRegex().find(serviceName)
                match?.value ?: serviceName
            }
            else -> serviceName
        }
    }

    private fun calculateSignalStrength(): Int {
        // Android NSD doesn't provide signal strength, return a random value between 60-90 to simulate it
        return (60..90).random()
    }
}

// extension function for the AddDeviceScreen to use real discovery
suspend fun startRealDeviceDiscovery(
    context: Context,
    onDevicesFound: (List<DiscoveredDevice>) -> Unit
) {
    val discoveryManager = MDNSDiscoveryManager(context)

    discoveryManager.discoverDevices().collect { devices ->
        onDevicesFound(devices)
    }
}