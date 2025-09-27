package com.example.aqualuminus_rebuild.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ActivityLog(
    val id: String = "",
    val deviceId: String = "",
    val eventType: String = "GENERAL", // e.g., "UV_STATUS", "CONNECTION"
    val description: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)