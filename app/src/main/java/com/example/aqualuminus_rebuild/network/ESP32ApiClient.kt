package com.example.aqualuminus_rebuild.network

import com.example.aqualuminus_rebuild.data.services.DeviceApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class ESP32ApiClient private constructor() {

    // a single, reusable OkHttpClient for all connections
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    // cache for service instances to avoid recreating them for the same IP
    private val serviceCache = ConcurrentHashMap<String, DeviceApiService>()

    private fun getService(ipAddress: String, port: Int): DeviceApiService {
        val baseUrl = "http://$ipAddress:$port/"
        return serviceCache.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeviceApiService::class.java)
        }
    }

    suspend fun getDeviceStatus(ipAddress: String, port: Int = 80): DeviceStatus {
        return getService(ipAddress, port).getStatus()
    }

    suspend fun turnUVOn(ipAddress: String, port: Int = 80): UVResponse {
        return getService(ipAddress, port).turnOn()
    }

    suspend fun turnUVOff(ipAddress: String, port: Int = 80): UVResponse {
        return getService(ipAddress, port).turnOff()
    }

    suspend fun getDeviceInfo(ipAddress: String, port: Int = 80): DeviceInfo {
        return getService(ipAddress, port).getInfo()
    }

    suspend fun getSensorData(ipAddress: String, port: Int = 80): SensorDataResponse {
        return getService(ipAddress, port).getSensorData()
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