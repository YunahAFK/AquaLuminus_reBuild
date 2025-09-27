package com.example.aqualuminus_rebuild.data.services

import com.example.aqualuminus_rebuild.network.DeviceInfo
import com.example.aqualuminus_rebuild.network.DeviceStatus
import com.example.aqualuminus_rebuild.network.SensorDataResponse
import com.example.aqualuminus_rebuild.network.UVResponse
import retrofit2.http.GET
import retrofit2.http.POST

interface DeviceApiService {
    @GET("api/status")
    suspend fun getStatus(): DeviceStatus

    @POST("api/on")
    suspend fun turnOn(): UVResponse

    @POST("api/off")
    suspend fun turnOff(): UVResponse

    @GET("api/info")
    suspend fun getInfo(): DeviceInfo

    @GET("api/sensors")
    suspend fun getSensorData(): SensorDataResponse
}