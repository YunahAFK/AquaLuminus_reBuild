package com.example.aqualuminus_rebuild.ui.screens.iot

import com.example.aqualuminus_rebuild.data.models.AquaLuminusDevice

data class DeviceControlUiState(
    val device: AquaLuminusDevice? = null,
    val isLoading: Boolean = false,
    val isUpdatingUV: Boolean = false,
    val errorMessage: String? = null
)
