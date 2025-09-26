package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.Activity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val ACTIVITIES_COLLECTION = "activities"
    }

    // ✅ Get the 10 most recent activities
    suspend fun getRecentActivities(): Result<List<Activity>> {
        return try {
            val snapshot = firestore.collection(ACTIVITIES_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
            Result.success(snapshot.toObjects())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createActivity(activity: Activity): Result<Unit> {
        return try {
            firestore.collection(ACTIVITIES_COLLECTION).add(activity).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}