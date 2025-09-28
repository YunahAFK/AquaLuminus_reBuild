package com.example.aqualuminus_rebuild.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aqualuminus_rebuild.data.factory.ServiceFactory
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository
import com.example.aqualuminus_rebuild.network.ESP32ApiClient

class UVTurnOffWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UVTurnOffWorker"
    }

    private val deviceRepository = DeviceRepository.getInstance(applicationContext)
    private val apiClient = ESP32ApiClient.getInstance()

    override suspend fun doWork(): Result {
        return try {
            val deviceId = inputData.getString("device_id") ?: "Unknown"
            val scheduleId = inputData.getString("schedule_id") ?: "Unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            val ipAddress = inputData.getString("ip_address") ?: return Result.failure()
            val port = inputData.getInt("port", 80)

            Log.d(TAG, "Turning off UV light for: $scheduleName")

            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)

            apiClient.turnUVOff(ipAddress, port)

            val device = deviceRepository.getDevice(deviceId)
            val currentTime = System.currentTimeMillis()
            val uvDuration = device?.uvStartTime?.let { currentTime - it } ?: 0L

            deviceRepository.updateDeviceUVStatus(
                deviceId = deviceId,
                isUVOn = false,
                uvEndTime = currentTime,
                additionalUVTime = uvDuration
            )

            Log.d(TAG, "UV light turned off successfully for $scheduleName")
            notificationManager.showCompletionNotification(scheduleId, scheduleName)
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off UV light with exception", e)
            val scheduleId = inputData.getString("schedule_id") ?: "Unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)
            notificationManager.showErrorNotification(
                scheduleId,
                scheduleName,
                "Error turning off UV light: ${e.message}"
            )
            Result.retry()
        }
    }
}