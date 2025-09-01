package com.example.mad_assignment.data.datasource

import android.util.Log
import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.PackageStatus
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class TravelPackageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getTravelPackages(): Flow<List<TravelPackage>> {
        return firestore
            .collection(TRAVEL_PACKAGE_COLLECTION)
            .whereEqualTo("status", PackageStatus.AVAILABLE.name)
            .dataObjects()

    }

    suspend fun getTravelPackage(packageId: String): TravelPackage? {
        return firestore
            .collection(TRAVEL_PACKAGE_COLLECTION)
            .document(packageId)
            .get()
            .await()
            .toObject<TravelPackage>()
    }

    suspend fun create(newPackage: TravelPackage): String {
        return firestore.collection(TRAVEL_PACKAGE_COLLECTION).add(newPackage).await().id
    }

    suspend fun delete(packageId: String) {
        firestore.collection(TRAVEL_PACKAGE_COLLECTION).document(packageId).delete().await()
    }

    fun getDepartureDates(packageId: String): Flow<List<DepartureDate>> {
        return firestore
            .collection(TRAVEL_PACKAGE_COLLECTION)
            .document(packageId)
            .collection("departures")
            .dataObjects()
    }

    suspend fun getTripsByIds(ids: List<String>): List<Trip> {
        if (ids.isEmpty()) return emptyList()

        try {
            val snapshot = firestore.collection("trips")
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .await()

            // --- TEMPORARY DEBUGGING CODE ---
            Log.d("DataSourceDebug", "Query successful. Found ${snapshot.size()} documents.")
            for (document in snapshot.documents) {
                Log.d("DataSourceDebug", "Doc ID: ${document.id}, Data: ${document.data}")
            }
            // --- END OF DEBUGGING CODE ---

            // This is the line that might be failing silently
            val trips = snapshot.toObjects<Trip>()
            Log.d("DataSourceDebug", "Successfully mapped ${trips.size} Trip objects.")

            val tripsById = trips.associateBy { trip -> trip.tripId }
            return ids.mapNotNull { id -> tripsById[id] }

        } catch (e: Exception) {
            Log.e("DataSourceDebug", "Query FAILED with exception", e)
            return emptyList()
        }
    }

    companion object{
        private const val TRAVEL_PACKAGE_COLLECTION = "travel_packages"
        private const val TRIPS_COLLECTIOM = "trips"
    }
}