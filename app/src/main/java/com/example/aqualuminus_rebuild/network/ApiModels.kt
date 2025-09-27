package com.example.aqualuminus_rebuild.network

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

data class DeviceStatus(
    val uvLightOn: Boolean,
    val status: String,
    val timestamp: Long
)

data class UVResponse(
    val success: Boolean,
    val uvLightOn: Boolean? = null,
    val message: String,
    val timestamp: Long
)

data class DeviceInfo(
    val device: String?,
    val device_id: String?,
    val device_name: String?,
    val version: String?,
    val ip: String?,
    val mac: String?,
    val hostname: String?
)

data class SensorDataResponse(
    val temperature_c: Float?,
    val ph: Float?,
    val ph_voltage: Float?,
    val turbidity_raw: Int?
)