package com.example.aqualuminus_rebuild.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository

class SensorUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val deviceRepository = DeviceRepository.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        return try {
            deviceRepository.refreshDevices()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}