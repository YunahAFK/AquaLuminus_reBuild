package com.example.aqualuminus_rebuild.ui.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.manager.AuthState
import com.example.aqualuminus_rebuild.data.manager.AuthStateManager
import com.example.aqualuminus_rebuild.data.models.AquaLuminusDevice
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository
import com.example.aqualuminus_rebuild.data.repository.FirebaseAuthRepository
import com.example.aqualuminus_rebuild.ui.screens.iot.DiscoveredDevice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AquaLuminusDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val authStateManager = AuthStateManager.getInstance()
    private val authRepository = FirebaseAuthRepository()
    private val deviceRepository = DeviceRepository.getInstance(application)

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ General Variables --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    private val _uiState = MutableStateFlow(AquaLuminusDashboardUiState())
    val uiState: StateFlow<AquaLuminusDashboardUiState> = _uiState.asStateFlow()

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ General Variables --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard Variables --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    private val _loggedOut = MutableSharedFlow<Unit>()
    val loggedOut: SharedFlow<Unit> = _loggedOut.asSharedFlow()

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard Variables --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    val devices = deviceRepository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        observeAuthState()
        loadDevices()
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard Functions --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    private fun observeAuthState() {
        viewModelScope.launch {
            authStateManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _uiState.update { currentState ->
                            currentState.copy(
                                userName = state.user.displayName ?: "⟡⋆⭒Yun-ah⋆⭒⟡",
                                userPhotoUrl = state.user.photoUrl?.toString(),
                                isEmailVerified = state.user.isEmailVerified
                            )
                        }
                    }
                    AuthState.Unauthenticated -> {
                        _uiState.update { currentState ->
                            currentState.copy(
                                userName = "Guest",
                                userPhotoUrl = null
                            )
                        }
                        _loggedOut.tryEmit(Unit)
                    }
                }
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            val result = authRepository.sendVerificationEmail()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(successMessage = "Verification Email Sent!") }
                },
                onFailure = {
                    _uiState.update { it.copy(errorMessage = "Failed to send email. Please try again.") }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authStateManager.signOut()
                _uiState.update { currentState ->
                    currentState.copy(
                        devices = emptyList(),
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        errorMessage = "Logout failed: ${e.message}"
                    )
                }
            }
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard Functions --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DeviceListCard Functions --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDevices = true, errorMessage = null) }
            try {
                deviceRepository.getAllDevices()
                    .collect { devices ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                devices = devices,
                                isLoadingDevices = false,
                                errorMessage = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoadingDevices = false,
                        errorMessage = "failed to load devices: ${e.message}"
                    )
                }
            }
        }
    }

    fun refreshDevices() = viewModelScope.launch {
        _uiState.update { it.copy(isLoadingDevices = true) }
        try {
            deviceRepository.refreshDevices()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "failed to refresh devices: ${e.message}") }
        } finally {
            _uiState.update { it.copy(isLoadingDevices = false) }
        }
    }

    fun addDevice(discoveredDevice: DiscoveredDevice) {
        viewModelScope.launch {
            Log.d("DashboardVM","addDevice called for ${discoveredDevice.id}")
            try {
                _uiState.update { it.copy(isLoadingDevices = true, errorMessage = null) }
                val aqua = discoveredDevice.toAquaLuminusDevice()
                deviceRepository.addDevice(aqua)
                Log.d("DashboardVM","deviceRepository.addDevice completed for ${aqua.id}")
                _uiState.update { it.copy(isLoadingDevices = false) }
            } catch (e: Exception) {
                Log.e("DashboardVM","addDevice failed", e)
                _uiState.update {
                    it.copy(isLoadingDevices = false,
                        errorMessage = "failed to add device: ${e.message}")
                }
            }
        }
    }

    fun removeDevice(deviceId: String) {
        viewModelScope.launch {
            try {
                deviceRepository.removeDevice(deviceId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "failed to remove device: ${e.message}") }
            }
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DeviceListCard Functions --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ Utility Functions --start-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        // clean up any resources if needed
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ Utility Functions --end-- ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
}

private fun DiscoveredDevice.toAquaLuminusDevice(): AquaLuminusDevice {
    return AquaLuminusDevice(
        id = this.id,
        name = this.name,
        ipAddress = this.ipAddress,
        port = 80,
        version = "1.0",
        isOnline = this.isAvailable,
        isUVOn = false,
        lastSeen = System.currentTimeMillis()
    )
}