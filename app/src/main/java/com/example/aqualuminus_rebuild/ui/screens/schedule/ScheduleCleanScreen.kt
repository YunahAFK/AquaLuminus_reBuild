@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.aqualuminus_rebuild.ui.screens.schedule

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository
import com.example.aqualuminus_rebuild.ui.screens.schedule.components.DaySelector
import com.example.aqualuminus_rebuild.ui.screens.schedule.components.DeviceSelector
import com.example.aqualuminus_rebuild.ui.screens.schedule.components.DurationPicker
import com.example.aqualuminus_rebuild.ui.screens.schedule.components.ScheduleBottomBar
import com.example.aqualuminus_rebuild.ui.screens.schedule.components.ScheduleNameInput
import com.example.aqualuminus_rebuild.ui.screens.schedule.components.TimePicker

@Composable
fun ScheduleCleanScreen(
    scheduleId: String? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ScheduleCleanViewModel = viewModel(
        factory = ScheduleCleanViewModelFactory(context = context)
    )

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ STATE COLLECTION ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    val formState by viewModel.formState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingSchedule by viewModel.isLoadingSchedule.collectAsState()
    val currentSchedule by viewModel.currentSchedule.collectAsState()
    val saveResult by viewModel.saveResult.collectAsState()
    val error by viewModel.error.collectAsState()

    val deviceRepository = DeviceRepository.getInstance(context)
    val devices by deviceRepository.getAllDevices().collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = scheduleId != null

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ SIDE EFFECTS ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    // load schedule data when entering edit mode
    LaunchedEffect(scheduleId) {
        if (isEditMode) {
            viewModel.loadSchedule(scheduleId!!)
        }
    }

    // populate the form once the schedule data is loaded
    LaunchedEffect(currentSchedule) {
        currentSchedule?.let {
            viewModel.populateFormForEdit(it)
        }
    }

    // show a snackbar on save success or failure
    LaunchedEffect(saveResult) {
        when (val result = saveResult) {
            is ScheduleCleanViewModel.SaveResult.Success -> {
                val message = if (isEditMode) "Schedule Updated!" else "Schedule Saved!"
                snackbarHostState.showSnackbar(message)
                viewModel.clearSaveResult()
                viewModel.clearCurrentSchedule()
                onBackClick()
            }
            is ScheduleCleanViewModel.SaveResult.Error -> {
                snackbarHostState.showSnackbar("Error: ${result.message}")
                viewModel.clearSaveResult()
            }
            null -> { /* min_ju is sleeping */ }
        }
    }

    // show a snackbar for any other general errors
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar("Error: $it")
            viewModel.clearError()
        }
    }

    // clean up the ViewModel state when the screen is left
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentSchedule()
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ EVENT HANDLERS ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    val onSaveClick = {
        val schedule = buildScheduleFromState(
            formState = formState,
            isEditMode = isEditMode,
            scheduleId = scheduleId,
            currentIsActive = currentSchedule?.isActive
        )

        if (isEditMode) {
            viewModel.updateSchedule(schedule)
        } else {
            viewModel.saveSchedule(schedule)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Schedule" else "Schedule Cleaning",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ScheduleBottomBar(
                onCancelClick = onBackClick,
                onSaveClick = onSaveClick,
                isSaveEnabled = formState.selectedDays.isNotEmpty() && formState.selectedDeviceId != null && !isLoading && !isLoadingSchedule,
                saveButtonText = if (isEditMode) "Update" else "Save"
            )
        }
    ) { padding ->
        if (isLoadingSchedule && isEditMode) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading schedule...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                ScheduleNameInput(
                    scheduleName = formState.scheduleName,
                    onScheduleNameChanged = viewModel::onScheduleNameChanged
                )

                DeviceSelector(
                    devices = devices,
                    selectedDeviceIds = setOf(formState.selectedDeviceId).filterNotNull().toSet(),
                    onDeviceSelected = viewModel::onDeviceSelected
                )

                DaySelector(
                    selectedDays = formState.selectedDays,
                    onDaysChanged = viewModel::onDaysChanged
                )

                TimePicker(
                    selectedHour = formState.selectedHour,
                    selectedMinute = formState.selectedMinute,
                    selectedAmPm = formState.selectedAmPm,
                    onHourChanged = viewModel::onHourChanged,
                    onMinuteChanged = viewModel::onMinuteChanged,
                    onAmPmChanged = viewModel::onAmPmChanged
                )

                DurationPicker(
                    selectedMinutes = formState.selectedDuration,
                    onMinutesChanged = viewModel::onDurationChanged
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * a helper function to create a [SavedSchedule] object from the form's state.
 */
private fun buildScheduleFromState(
    formState: ScheduleFormState,
    isEditMode: Boolean,
    scheduleId: String?,
    currentIsActive: Boolean?
): SavedSchedule {
    // convert 12-hour format with AM/PM to 24-hour format for storage
    val hour24 = when {
        formState.selectedAmPm == 0 && formState.selectedHour == 12 -> 0 // 12 AM is 00:00
        formState.selectedAmPm == 1 && formState.selectedHour != 12 -> formState.selectedHour + 12
        else -> formState.selectedHour
    }

    val timeString = String.format("%02d:%02d", hour24, formState.selectedMinute)
    val daysFullNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDayNames = formState.selectedDays.sorted().map { daysFullNames[it] }

    val newSchedule = SavedSchedule(
        id = if (isEditMode) scheduleId!! else "",
        deviceId = formState.selectedDeviceId ?: "",
        name = formState.scheduleName.ifBlank { "Untitled Schedule" },
        days = selectedDayNames,
        time = timeString,
        durationMinutes = formState.selectedDuration,
        isActive = currentIsActive ?: true // preserve active state on edit, default to true for new
    )

    Log.d("ScheduleCleanScreen", "Built Schedule: $newSchedule")
    return newSchedule
}


@Preview(showBackground = true)
@Composable
private fun PreviewScheduleCleanScreen() {
    MaterialTheme {
        ScheduleCleanScreen()
    }
}