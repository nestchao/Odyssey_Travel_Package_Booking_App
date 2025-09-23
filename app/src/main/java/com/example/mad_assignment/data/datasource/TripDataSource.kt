package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.Trip
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAllTrips(): Flow<List<Trip>> {
        return firestore.collection(TRIPS_COLLECTION)
            .whereEqualTo("deletedAt", null)
            .dataObjects()
    }

    suspend fun getTripsByIds(tripIds: List<String>): List<Trip> {
        if (tripIds.isEmpty()) {
            return emptyList()
        }
        return firestore.collection(TRIPS_COLLECTION)
            .whereIn(FieldPath.documentId(), tripIds)
            .get()
            .await()
            .toObjects(Trip::class.java)
    }

    suspend fun addTrip(trip: Trip): String {
        val tripDocRef = firestore.collection(TRIPS_COLLECTION).document()
        val finalTrip = trip.copy(tripId = tripDocRef.id)
        tripDocRef.set(finalTrip).await()
        return tripDocRef.id
    }

    suspend fun updateTrip(trip: Trip) {
        if (trip.tripId.isEmpty()) {
            throw IllegalArgumentException("Trip ID cannot be empty for an update.")
        }
        val tripDocRef = firestore.collection(TRIPS_COLLECTION).document(trip.tripId)
        // .set() will overwrite the document completely, which is what we want for an update.
        tripDocRef.set(trip).await()
    }

    suspend fun softDeleteTrip(tripId: String) {
        val tripDocRef = firestore.collection(TRIPS_COLLECTION).document(tripId)
        tripDocRef.update("deletedAt", Timestamp.now()).await()
    }

    companion object {
        const val TRIPS_COLLECTION = "trips"
    }
}