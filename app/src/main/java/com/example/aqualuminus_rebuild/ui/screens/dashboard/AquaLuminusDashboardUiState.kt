package com.example.aqualuminus_rebuild.ui.screens.dashboard

import com.example.aqualuminus_rebuild.data.models.AquaLuminusDevice

data class AquaLuminusDashboardUiState(

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ User Information ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    val userName: String = "｡⋆Yun-ah⟡˚",
    val userPhotoUrl: String? = null,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ AquaLuminus Devices ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    val devices: List<AquaLuminusDevice> = emptyList(),
    val isLoadingDevices: Boolean = false,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ UI State ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)