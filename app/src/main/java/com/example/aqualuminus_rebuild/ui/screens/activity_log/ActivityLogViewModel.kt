package com.example.aqualuminus_rebuild.ui.screens.activity_log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqualuminus_rebuild.data.repository.ActivityLogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ActivityLogViewModel : ViewModel() {
    private val repository = ActivityLogRepository()

    val logs = repository.observeLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}