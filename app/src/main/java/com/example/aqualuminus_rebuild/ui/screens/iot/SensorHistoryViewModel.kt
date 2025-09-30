package com.example.aqualuminus_rebuild.ui.screens.iot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.models.SensorHistory
import com.example.aqualuminus_rebuild.data.repository.SensorHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorHistoryViewModel : ViewModel() {
    private val repository = SensorHistoryRepository()

    private val _history = MutableStateFlow<List<SensorHistory>>(emptyList())
    val history: StateFlow<List<SensorHistory>> = _history.asStateFlow()

    fun fetchHistory(deviceId: String) {
        viewModelScope.launch {
            _history.value = repository.getSensorHistory(deviceId)
        }
    }
}