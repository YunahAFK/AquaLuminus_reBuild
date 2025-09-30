package com.example.aqualuminus_rebuild.data.repository

import android.util.Log
import com.example.aqualuminus_rebuild.data.models.SensorHistory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class SensorHistoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val historyCollection = firestore.collection("sensor_history")

    suspend fun addSensorHistory(history: SensorHistory) {
        try {
            historyCollection.add(history).await()
        } catch (e: Exception) {
            Log.e("SensorHistoryRepository", "Error adding sensor history", e)
        }
    }

    suspend fun getSensorHistory(deviceId: String): List<SensorHistory> {
        return try {
            val snapshot = historyCollection
                .whereEqualTo("deviceId", deviceId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            snapshot.toObjects(SensorHistory::class.java)
        } catch (e: Exception) {
            Log.e("SensorHistoryRepository", "Error getting sensor history", e)
            emptyList()
        }
    }
}