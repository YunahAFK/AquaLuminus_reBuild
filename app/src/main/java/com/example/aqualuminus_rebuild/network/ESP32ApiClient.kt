package com.example.aqualuminus_rebuild.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

interface ESP32ApiService {
    @GET("api/status")
    suspend fun getStatus(): DeviceStatus

    @POST("api/on")
    suspend fun turnOn(): UVResponse

    @POST("api/off")
    suspend fun turnOff(): UVResponse

    @GET("api/info")
    suspend fun getInfo(): DeviceInfo
}

class ESP32ApiClient {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun createService(baseUrl: String): ESP32ApiService {
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ESP32ApiService::class.java)
    }

    suspend fun getDeviceStatus(ipAddress: String, port: Int = 80): DeviceStatus {
        val service = createService("http://$ipAddress:$port")
        return service.getStatus()
    }

    suspend fun turnUVOn(ipAddress: String, port: Int = 80): UVResponse {
        val service = createService("http://$ipAddress:$port")
        return service.turnOn()
    }

    suspend fun turnUVOff(ipAddress: String, port: Int = 80): UVResponse {
        val service = createService("http://$ipAddress:$port")
        return service.turnOff()
    }

    suspend fun getDeviceInfo(ipAddress: String, port: Int = 80): DeviceInfo {
        val service = createService("http://$ipAddress:$port")
        return service.getInfo()
    }

    companion object {
        @Volatile
        private var INSTANCE: ESP32ApiClient? = null

        fun getInstance(): ESP32ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ESP32ApiClient().also { INSTANCE = it }
            }
        }
    }
}