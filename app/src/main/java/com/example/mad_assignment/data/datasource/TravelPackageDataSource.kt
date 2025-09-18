package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Singleton
class TravelPackageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val PACKAGES_COLLECTION = "packages"
        private const val DEPARTURE_DATES_COLLECTION = "departureDates"
        private const val TRIPS_COLLECTION = "trips"
    }

    suspend fun getTravelPackages(): Result<List<TravelPackage>> {
        return try {
            val snapshot = firestore.collection(PACKAGES_COLLECTION)
                .get()
                .await().toObjects<TravelPackage>()
            Result.success(snapshot)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPackageById(packageId: String): Result<TravelPackage?> {
        return try {
            val document = firestore.collection(PACKAGES_COLLECTION).document(packageId).get().await()
            val travelPackage = document.toObject(TravelPackage::class.java)
            Result.success(travelPackage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDepartureDatesForPackage(packageId: String): Result<List<DepartureDate>> {
        return try {
            val snapshot = firestore.collection(DEPARTURE_DATES_COLLECTION)
                .whereEqualTo("packageId", packageId)
                .get()
                .await()
            val dates = snapshot.toObjects(DepartureDate::class.java)
            val sortedDates = dates.sortedBy { it.startDate }
            Result.success(sortedDates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripById(tripId: String): Result<Trip?> {
        return try {
            val document = firestore.collection(TRIPS_COLLECTION).document(tripId).get().await()
            val trip = document.toObject(Trip::class.java)
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPackage(newPackage: TravelPackage): Result<String> {
        return try {
            val documentRef = firestore.collection(PACKAGES_COLLECTION).add(newPackage).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePackage(packageId: String): Result<Unit> {
        return try {
            firestore.collection(PACKAGES_COLLECTION).document(packageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripsByIds(ids: List<String>): Result<List<Trip>> {
        if (ids.isEmpty()) {
            return Result.success(emptyList())
        }
        return try {
            val snapshot = firestore.collection(TRIPS_COLLECTION)
                .whereIn(FieldPath.documentId(), ids)
                .get()
                .await()
            val trips = snapshot.toObjects(Trip::class.java)
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}