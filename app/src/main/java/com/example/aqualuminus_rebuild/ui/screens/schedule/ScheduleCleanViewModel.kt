package com.example.aqualuminus_rebuild.ui.screens.schedule

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.factory.ServiceFactory
import com.example.aqualuminus_rebuild.data.repository.ScheduleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * data class to hold the state of the schedule creation/editing form.
 */
data class ScheduleFormState(
    val scheduleName: String = "",
    val selectedDeviceId: String? = null,
    val selectedDays: Set<Int> = emptySet(),
    val selectedHour: Int = Calendar.getInstance().get(Calendar.HOUR),
    val selectedMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val selectedAmPm: Int = if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) 0 else 1,
    val selectedDuration: Int = 30
)

class ScheduleCleanViewModel(
    private val repository: ScheduleRepository,
    private val context: Context
) : ViewModel() {

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DEPENDENCIES ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    private val scheduleService = ServiceFactory.getScheduleService(context)
    private val timeCalculator = ServiceFactory.getTimeCalculator()

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ STATES ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    private val _schedules = MutableStateFlow<List<SavedSchedule>>(emptyList())
    val schedules: StateFlow<List<SavedSchedule>> = _schedules.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // --- State for ScheduleCleanScreen ---
    private val _formState = MutableStateFlow(ScheduleFormState())
    val formState: StateFlow<ScheduleFormState> = _formState.asStateFlow()

    private val _currentSchedule = MutableStateFlow<SavedSchedule?>(null)
    val currentSchedule: StateFlow<SavedSchedule?> = _currentSchedule.asStateFlow()

    private val _isLoadingSchedule = MutableStateFlow(false)
    val isLoadingSchedule: StateFlow<Boolean> = _isLoadingSchedule.asStateFlow()

    private val _saveResult = MutableStateFlow<SaveResult?>(null)
    val saveResult: StateFlow<SaveResult?> = _saveResult.asStateFlow()

    init {
        observeSchedules()
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ EVENT HANDLERS ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    fun onScheduleNameChanged(name: String) { _formState.update { it.copy(scheduleName = name) } }
    fun onDeviceSelected(deviceId: String) { _formState.update { it.copy(selectedDeviceId = if (it.selectedDeviceId == deviceId) null else deviceId) } }
    fun onDaysChanged(days: Set<Int>) { _formState.update { it.copy(selectedDays = days) } }
    fun onHourChanged(hour: Int) { _formState.update { it.copy(selectedHour = hour) } }
    fun onMinuteChanged(minute: Int) { _formState.update { it.copy(selectedMinute = minute) } }
    fun onAmPmChanged(amPm: Int) { _formState.update { it.copy(selectedAmPm = amPm) } }
    fun onDurationChanged(duration: Int) { _formState.update { it.copy(selectedDuration = duration) } }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ DATA OPERATIONS ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */
    private fun observeSchedules() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.observeSchedules()
                .catch { exception ->
                    _error.value = exception.message ?: "Failed to observe schedules"
                }
                .collect { scheduleList ->
                    _schedules.value = scheduleList.map { schedule ->
                        schedule.copy(nextRun = calculateNextRun(schedule))
                    }
                    _isLoading.value = false
                }
        }
    }

    fun saveSchedule(schedule: SavedSchedule) {
        viewModelScope.launch {
            _isLoading.value = true
            val scheduleWithId = schedule.copy(id = UUID.randomUUID().toString())
            repository.saveSchedule(scheduleWithId).fold(
                onSuccess = {
                    if (scheduleWithId.isActive) {
                        scheduleService.scheduleUVCleaning(context, scheduleWithId)
                    }
                    _saveResult.value = SaveResult.Success
                },
                onFailure = { _saveResult.value = SaveResult.Error(it.message ?: "Failed to save") }
            )
            _isLoading.value = false
        }
    }

    fun updateSchedule(schedule: SavedSchedule) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateSchedule(schedule).fold(
                onSuccess = {
                    scheduleService.cancelSchedule(context, schedule.id)
                    if (schedule.isActive) {
                        scheduleService.scheduleUVCleaning(context, schedule)
                    }
                    _saveResult.value = SaveResult.Success
                },
                onFailure = { _saveResult.value = SaveResult.Error(it.message ?: "Failed to update") }
            )
            _isLoading.value = false
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            scheduleService.cancelSchedule(context, scheduleId)
            repository.deleteSchedule(scheduleId).onFailure {
                _error.value = it.message ?: "Failed to delete schedule"
            }
        }
    }

    fun updateScheduleStatus(scheduleId: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateScheduleStatus(scheduleId, isActive).onSuccess {
                val schedule = _schedules.value.find { it.id == scheduleId }
                if (schedule != null) {
                    if (isActive) {
                        scheduleService.scheduleUVCleaning(context, schedule.copy(isActive = true))
                    } else {
                        scheduleService.cancelSchedule(context, scheduleId)
                    }
                }
            }.onFailure {
                _error.value = it.message ?: "Failed to update status"
            }
        }
    }

    fun loadSchedule(scheduleId: String) {
        viewModelScope.launch {
            _isLoadingSchedule.value = true
            val schedule = repository.getScheduleById(scheduleId)
            _currentSchedule.value = schedule
            if (schedule == null) _error.value = "Schedule not found"
            _isLoadingSchedule.value = false
        }
    }

    fun populateFormForEdit(schedule: SavedSchedule) {
        val (hour, minute, amPm) = parseAndConvertTime(schedule.time)
        val daysFullNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        _formState.update {
            it.copy(
                scheduleName = schedule.name,
                selectedDeviceId = schedule.deviceId,
                selectedDuration = schedule.durationMinutes,
                selectedHour = hour,
                selectedMinute = minute,
                selectedAmPm = amPm,
                selectedDays = schedule.days.mapNotNull { dayName ->
                    daysFullNames.indexOf(dayName).takeIf { i -> i >= 0 }
                }.toSet()
            )
        }
    }

    /* ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ UTILITY & CLEANUP ⟡ ⋆⭒˚｡⋆Yun-ah⟡ ⋆⭒˚｡⋆ */

    private fun parseAndConvertTime(timeString: String): Triple<Int, Int, Int> {
        val timeParts = timeString.split(":")
        if (timeParts.size != 2) return Triple(12, 0, 0)

        val hour24 = timeParts[0].toIntOrNull() ?: 12
        val minute = timeParts[1].toIntOrNull() ?: 0

        val amPm = if (hour24 >= 12) 1 else 0 // 0 = AM, 1 = PM
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return Triple(hour12, minute, amPm)
    }

    private fun calculateNextRun(schedule: SavedSchedule): String {
        if (!schedule.isActive) return "Paused"
        return try {
            val nextRunTime = timeCalculator.calculateNextRunTime(schedule.days, schedule.time)
            val daysDiff = ((nextRunTime - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
            val (hour, minute) = timeCalculator.parseTime(schedule.time)

            when (daysDiff) {
                0 -> "Today at ${formatTime(hour, minute)}"
                1 -> "Tomorrow at ${formatTime(hour, minute)}"
                else -> {
                    val nextRunCalendar = Calendar.getInstance().apply { timeInMillis = nextRunTime }
                    val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(nextRunCalendar.time)
                    "$dayName at ${formatTime(hour, minute)}"
                }
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
    }

    fun clearError() { _error.value = null }
    fun clearSaveResult() { _saveResult.value = null }
    fun clearCurrentSchedule() { _currentSchedule.value = null }

    sealed class SaveResult {
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}

class ScheduleCleanViewModelFactory(
    private val repository: ScheduleRepository = ScheduleRepository(),
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleCleanViewModel::class.java)) {
            return ScheduleCleanViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}