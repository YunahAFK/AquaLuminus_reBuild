package com.example.aqualuminus_rebuild.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aqualuminus_rebuild.data.models.AquaLuminusDevice
import com.example.aqualuminus_rebuild.ui.screens.dashboard.components.DeviceListCard
import com.example.aqualuminus_rebuild.ui.screens.dashboard.components.HeaderCard
import com.example.aqualuminus_rebuild.ui.screens.dashboard.components.QuickActionCard

@Composable
fun AquaLuminusDashboardScreen(
    aquaLuminusDashboardViewModel: AquaLuminusDashboardViewModel = viewModel(),
    onLoggedOut: () -> Unit,
    onProfileClick: () -> Unit,
    onAddDevice: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onScheduleCleanClick: () -> Unit = {},
    onActivityLogClick: () -> Unit = {}
) {
    val uiState by aquaLuminusDashboardViewModel.uiState.collectAsState()
    val devices by aquaLuminusDashboardViewModel.devices.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        aquaLuminusDashboardViewModel.loggedOut.collect {
            onLoggedOut()
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        val message = uiState.successMessage ?: uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            aquaLuminusDashboardViewModel.clearMessages()
        }
    }

    LaunchedEffect(Unit) {
        aquaLuminusDashboardViewModel.loadDevices()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        DashboardContent(
            modifier = Modifier.padding(paddingValues),

            uiState = uiState,

            /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
            onProfileClick = onProfileClick,
            onLogout = { aquaLuminusDashboardViewModel.logout() },
            onResendEmailClick = { aquaLuminusDashboardViewModel.resendVerificationEmail() },

            /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DeviceListCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
            devices = devices,
            onAddDevice = onAddDevice,
            onDeviceClick = onDeviceClick,
            onRefreshDevices = { aquaLuminusDashboardViewModel.refreshDevices() },

            /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ QuickActionCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

            onScheduleCleanClick = onScheduleCleanClick,
            onActivityLogClick = onActivityLogClick

        )
    }
}

@Composable
private fun DashboardContent(
    uiState: AquaLuminusDashboardUiState,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ HeaderCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onResendEmailClick: () -> Unit,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DeviceListCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    devices: List<AquaLuminusDevice>,
    onAddDevice: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onRefreshDevices: () -> Unit,

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ QuickActionCard ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    onScheduleCleanClick: () -> Unit,
    onActivityLogClick: () -> Unit,

    modifier: Modifier = Modifier

    ) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (!uiState.isEmailVerified) {
            VerificationBanner(onResendClick = onResendEmailClick)
        }

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

        QuickActionCard(
            onScheduleCleanClick = onScheduleCleanClick,
            onActivityLogClick = onActivityLogClick
        )
    }
}

@Composable
private fun VerificationBanner(onResendClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "   Check Email for Account Verification.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onResendClick) {
                Text("Resend", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}