package com.example.aqualuminus_rebuild.data.services

import com.example.aqualuminus_rebuild.network.DeviceInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST

interface UVLightService {
    @GET("api/status")
    suspend fun getStatus(): Response<UVLightResponse>

    @POST("api/on")
    suspend fun turnOn(): Response<UVLightResponse>

    @POST("api/off")
    suspend fun turnOff(): Response<UVLightResponse>

    @POST("api/toggle")
    suspend fun toggle(): Response<UVLightResponse>

    @GET("api/info")
    suspend fun getDeviceInfo(): Response<DeviceInfo>
}

data class UVLightResponse(
    val success: Boolean = true,
    val uvLightOn: Boolean,
    val status: String? = null,
    val message: String? = null,
    val timestamp: Long,
    val device: String? = null
)