package com.example.aqualuminus_rebuild.data.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SensorHistory(
    val deviceId: String = "",
    @ServerTimestamp
    val timestamp: Date? = null,
    val temperature: Float? = null,
    val ph: Float? = null
)