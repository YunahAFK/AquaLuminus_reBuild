package com.example.aqualuminus_rebuild.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aqualuminus_rebuild.data.models.SensorHistory
import com.example.aqualuminus_rebuild.data.repository.DeviceRepository
import com.example.aqualuminus_rebuild.data.repository.SensorHistoryRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

class HistoryLoggingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val deviceRepository = DeviceRepository.getInstance(applicationContext)
    private val sensorHistoryRepository = SensorHistoryRepository()

    override suspend fun doWork(): Result {
        return try {
            val devices = deviceRepository.getAllDevices().first()
            for (device in devices) {
                sensorHistoryRepository.addSensorHistory(
                    SensorHistory(
                        deviceId = device.id,
                        temperature = device.temperature,
                        ph = device.ph
                    )
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}