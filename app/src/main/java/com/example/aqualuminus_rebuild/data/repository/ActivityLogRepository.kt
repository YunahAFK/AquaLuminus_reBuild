package com.example.aqualuminus_rebuild.data.repository

import android.util.Log
import com.example.aqualuminus_rebuild.data.models.ActivityLog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ActivityLogRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val logsCollection = firestore.collection("activity_logs")

    suspend fun addLogEntry(deviceId: String, eventType: String, description: String) {
        try {
            val log = ActivityLog(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId,
                eventType = eventType,
                description = description
            )
            logsCollection.document(log.id).set(log).await()
        } catch (e: Exception) {
            Log.e("ActivityLogRepository", "Error adding log entry", e)
        }
    }

    fun observeLogs(): Flow<List<ActivityLog>> = callbackFlow {
        val listener = logsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Get the last 100 entries
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.toObjects(ActivityLog::class.java)
                    trySend(logs)
                }
            }
        awaitClose { listener.remove() }
    }
}