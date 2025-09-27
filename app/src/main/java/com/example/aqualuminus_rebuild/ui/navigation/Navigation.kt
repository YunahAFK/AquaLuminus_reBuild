package com.example.aqualuminus_rebuild.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.aqualuminus_rebuild.data.manager.AuthState
import com.example.aqualuminus_rebuild.ui.screens.activity_log.ActivityLogScreen
import com.example.aqualuminus_rebuild.ui.screens.auth.login.LoginScreen
import com.example.aqualuminus_rebuild.ui.screens.auth.register.RegisterScreen
import com.example.aqualuminus_rebuild.ui.screens.dashboard.AquaLuminusDashboardScreen
import com.example.aqualuminus_rebuild.ui.screens.dashboard.AquaLuminusDashboardViewModel
import com.example.aqualuminus_rebuild.ui.screens.iot.AddDeviceScreen
import com.example.aqualuminus_rebuild.ui.screens.iot.DeviceControlScreen
import com.example.aqualuminus_rebuild.ui.screens.iot.DeviceControlViewModel
import com.example.aqualuminus_rebuild.ui.screens.schedule.ScheduleListScreen
import com.example.aqualuminus_rebuild.ui.screens.schedule.ScheduleCleanScreen

sealed class Screen(val route: String) {
    object Login : Screen ("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object AddDevice : Screen("add_device")
    data object DeviceControl : Screen("device_control/{deviceId}") {
        fun createRoute(deviceId: String) = "device_control/$deviceId"
    }
    object SchedulesList : Screen("schedules_list")
    object ScheduleClean : Screen("schedule_clean")
    object ActivityLog : Screen("activity_log")

}

@Composable
fun NavGraph(
    authState: AuthState,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Dashboard.route
        is AuthState.Unauthenticated -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // firebase auth will trigger navigation, no manual navigation needed
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Dashboard.route) {
            val dashboardViewModel: AquaLuminusDashboardViewModel = viewModel()
            AquaLuminusDashboardScreen(
                aquaLuminusDashboardViewModel = dashboardViewModel,
                onLoggedOut = { navController.navigate(Screen.Login.route) { popUpTo(0) } },
                onAddDevice = { navController.navigate(Screen.AddDevice.route) },
                onDeviceClick = { id -> navController.navigate(Screen.DeviceControl.createRoute(id)) },
                onScheduleCleanClick = { navController.navigate(Screen.SchedulesList.route) },
                onActivityLogClick = { navController.navigate(Screen.ActivityLog.route) }
            )
        }

        composable(Screen.AddDevice.route) { backStackEntry ->
            val dashboardBackStackEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Dashboard.route)
            }
            val dashboardViewModel: AquaLuminusDashboardViewModel = viewModel(dashboardBackStackEntry)

            AddDeviceScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeviceSelected = { discoveredDevice ->
                    dashboardViewModel.addDevice(discoveredDevice)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.DeviceControl.route,
            arguments = listOf(
                navArgument("deviceId") {
                    type = NavType.StringType
                }
            )
        ) {
            val deviceControlViewModel: DeviceControlViewModel = viewModel()
            DeviceControlScreen(
                onBackClick = { navController.popBackStack() },
                deviceControlViewModel = deviceControlViewModel
            )
        }

        composable(Screen.SchedulesList.route) {
            ScheduleListScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onCreateNewClick = {
                    navController.navigate(Screen.ScheduleClean.route)
                },
                onEditScheduleClick = { scheduleId ->
                    // navigate to edit screen with schedule ID
                    navController.navigate("${Screen.ScheduleClean.route}?scheduleId=$scheduleId")
                }
            )
        }

        composable(
            route = "${Screen.ScheduleClean.route}?scheduleId={scheduleId}",
            arguments = listOf(
                navArgument("scheduleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val scheduleId = backStackEntry.arguments?.getString("scheduleId")
            ScheduleCleanScreen(
                scheduleId = scheduleId,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ActivityLog.route) {
            ActivityLogScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
