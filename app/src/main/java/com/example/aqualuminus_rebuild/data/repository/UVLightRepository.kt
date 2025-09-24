package com.example.aqualuminus_rebuild.data.repository

import android.content.Context
import com.example.aqualuminus_rebuild.data.services.UVLightResponse
import com.example.aqualuminus_rebuild.data.services.UVLightService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Response

class UVLightRepository (private val context: Context) {

    private var uvLightService: UVLightService? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: Flow<String> = _connectionStatus.asStateFlow()


    suspend fun turnOnUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService?.turnOn() }
    }

    suspend fun turnOffUVLight(): Result<Boolean> {
        return executeUVLightCommand { uvLightService?.turnOff() }
    }

    private suspend fun executeUVLightCommand(
        command: suspend () -> Response<UVLightResponse>?
    ): Result<Boolean> {
        return try {
            if (uvLightService == null) {
                return Result.failure(Exception("Not connected to device"))
            }

            val response = command()
            if (response?.isSuccessful == true && response.body() != null) {
                _isConnected.value = true
                val isOn = response.body()!!.uvLightOn
                Result.success(isOn)
            } else {
                _isConnected.value = false
                _connectionStatus.value = "Command Failed"
                Result.failure(Exception("Command Failed: HTTP ${response?.code()}"))
            }
        } catch (e: Exception) {
            _isConnected.value = false
            _connectionStatus.value = "Connection Lost"
            Result.failure(e)
        }
    }
}