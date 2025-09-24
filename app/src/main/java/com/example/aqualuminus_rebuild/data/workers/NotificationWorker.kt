package com.example.aqualuminus_rebuild.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aqualuminus_rebuild.data.factory.ServiceFactory

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val scheduleId = inputData.getString("schedule_id") ?: return Result.failure()
            val scheduleName = inputData.getString("schedule_name") ?: "UV Cleaning"
            val durationMinutes = inputData.getInt("duration_minutes", 30)
            val cleaningStartTime = inputData.getLong("cleaning_start_time", 0)

            Log.d(TAG, "showing advance notification for $scheduleName")

            // get notification manager from factory
            val notificationManager = ServiceFactory.getNotificationManager(applicationContext)

            notificationManager.showAdvanceNotification(
                scheduleId = scheduleId,
                scheduleName = scheduleName,
                durationMinutes = durationMinutes,
                cleaningStartTime = cleaningStartTime
            )

            Log.d(TAG, "advance notification sent successfully for $scheduleName")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "failed to show advance notification", e)
            Result.failure()
        }
    }
}