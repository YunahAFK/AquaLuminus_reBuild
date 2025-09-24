package com.example.aqualuminus_rebuild.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aqualuminus_rebuild.data.models.AquaLuminusDevice
import com.example.aqualuminus_rebuild.ui.screens.dashboard.components.DeviceListCard
import com.example.aqualuminus_rebuild.ui.screens.dashboard.components.HeaderCard

@Composable
fun AquaLuminusDashboardScreen(
    aquaLuminusDashboardViewModel: AquaLuminusDashboardViewModel = viewModel(),
    onLoggedOut: () -> Unit,
    onAddDevice: () -> Unit,
    onDeviceClick: (String) -> Unit
) {
    val uiState by aquaLuminusDashboardViewModel.uiState.collectAsState()
    val devices by aquaLuminusDashboardViewModel.devices.collectAsState()

    LaunchedEffect(Unit) {
        aquaLuminusDashboardViewModel.loggedOut.collect {
            onLoggedOut()
        }
    }

    LaunchedEffect(Unit) {
        aquaLuminusDashboardViewModel.loadDevices()
    }

    DashboardContent(
        uiState = uiState,

        /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
        onProfileClick = { TODO() },
        onLogout = { aquaLuminusDashboardViewModel.logout() },

        /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DeviceListCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
        devices = devices,
        onAddDevice = onAddDevice,
        onDeviceClick = onDeviceClick,
        onRefreshDevices = { aquaLuminusDashboardViewModel.refreshDevices() }
    )
}

@Composable
private fun DashboardContent(
    uiState: AquaLuminusDashboardUiState,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DeviceListCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    devices: List<AquaLuminusDevice>,
    onAddDevice: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onRefreshDevices: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderCard(
            userName = uiState.userName,
            userPhotoUrl = uiState.userPhotoUrl,
            onProfileClick = onProfileClick,
            onLogout = onLogout
        )
        DeviceListCard(
            devices = devices,
            isLoading = uiState.isLoadingDevices,
            onAddDevice = onAddDevice,
            onDeviceClick = onDeviceClick,
            onRefresh = onRefreshDevices
        )
    }
}
