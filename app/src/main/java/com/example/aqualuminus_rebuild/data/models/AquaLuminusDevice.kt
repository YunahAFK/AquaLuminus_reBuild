package com.example.aqualuminus_rebuild.data.models

data class AquaLuminusDevice(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 80,
    val hostname: String = "",
    val isOnline: Boolean = false,
    val isUVOn: Boolean = false,
    val uvStartTime: Long? = null,
    val uvEndTime: Long? = null,
    val totalUVTime: Long = 0,
    val version: String = "1.0",
    val lastSeen: Long = System.currentTimeMillis(),
    val deviceType: String = "AquaLuminus"
)