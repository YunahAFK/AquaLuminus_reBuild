package com.example.aqualuminus_rebuild.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aqualuminus_rebuild.data.factory.ServiceFactory
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository

class UVTurnOffWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "UVTurnOffWorker"
    }

    private val deviceRepository = DeviceRepository.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            val deviceId = inputData.getString("device_id") ?: "unknown"
            val scheduleId = inputData.getString("schedule_id") ?: "unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            Log.d(TAG, "turning off UV light for: $scheduleName")

            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)

            // Call the repository method
            val success = deviceRepository.turnUVOff(deviceId) // <-- CHANGE THIS
            if (success) {
                Log.d(TAG, "UV light turned off successfully for $scheduleName")
                notificationManager.showCompletionNotification(scheduleId, scheduleName)
                Result.success()
            } else {
                val errorMsg = "Failed to turn off UV light"
                Log.e(TAG, "$errorMsg for $scheduleName")
                notificationManager.showErrorNotification(scheduleId, scheduleName, errorMsg)
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to turn off UV light with exception", e)
            val scheduleId = inputData.getString("schedule_id") ?: "unknown"
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"

            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)
            notificationManager.showErrorNotification(
                scheduleId,
                scheduleName,
                "error turning off UV light: ${e.message}"
            )

            Result.retry()
        }
    }
}