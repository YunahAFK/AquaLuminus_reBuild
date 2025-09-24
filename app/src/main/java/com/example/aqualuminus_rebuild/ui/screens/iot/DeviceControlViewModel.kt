package com.example.aqualuminus_rebuild.ui.screens.iot

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeviceControlViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val deviceRepository = DeviceRepository.getInstance(application)
    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])

    private val _uiState = MutableStateFlow(DeviceControlUiState())
    val uiState: StateFlow<DeviceControlUiState> = _uiState.asStateFlow()

    init {
        Log.d("DeviceControlVM", "initializing with deviceId: $deviceId")
        loadDevice()
        observeDeviceChanges()
    }

    private fun loadDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                deviceRepository.getAllDevices().collect { devices ->
                    val device = devices.find { it.id == deviceId }
                    _uiState.update { currentState ->
                        currentState.copy(
                            device = device,
                            isLoading = false,
                            errorMessage = if (device == null) "device not found" else null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceControlVM", "error loading device", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "failed to load device: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeDeviceChanges() {
        viewModelScope.launch {
            deviceRepository.getAllDevices().collect { devices ->
                val updatedDevice = devices.find { it.id == deviceId }
                if (updatedDevice != null && updatedDevice != _uiState.value.device) {
                    _uiState.update { it.copy(device = updatedDevice) }
                }
            }
        }
    }

    fun toggleUV() {
        val currentDevice = _uiState.value.device ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingUV = true, errorMessage = null) }

            try {
                val success = if (currentDevice.isUVOn) {
                    deviceRepository.turnUVOff(deviceId)
                } else {
                    deviceRepository.turnUVOn(deviceId)
                }

                if (success) {
                    val updatedDevice = deviceRepository.getDevice(deviceId)
                    _uiState.update {
                        it.copy(device = updatedDevice)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "failed to toggle UV light. device may be offline."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceControlVM", "error toggling UV", e)
                _uiState.update {
                    it.copy(
                        errorMessage = "error: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isUpdatingUV = false) }
            }
        }
    }

    fun refreshDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                deviceRepository.refreshDevices()

                val refreshedDevice = deviceRepository.getDevice(deviceId)
                _uiState.update {
                    it.copy(
                        device = refreshedDevice,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("DeviceControlVM", "error refreshing device", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "failed to refresh device: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun removeDevice(onDeviceRemoved: () -> Unit) {
        viewModelScope.launch {
            try {
                deviceRepository.removeDevice(deviceId)
                onDeviceRemoved()
            } catch (e: Exception) {
                Log.e("DeviceControlVM", "error removing device", e)
                _uiState.update {
                    it.copy(errorMessage = "failed to remove device: ${e.message}")
                }
            }
        }
    }
}