package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.PackageStatus
import com.example.mad_assignment.data.model.TravelPackage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.dataObjects
import com.google.firebase.firestore.toObject
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

    fun getFeaturedPackages(): Flow<List<TravelPackage>> {
        return firestore
            .collection(TRAVEL_PACKAGE_COLLECTION)
            .whereEqualTo("isFeatured", true)
            .dataObjects()
    }

    fun getDepartureDates(packageId: String): Flow<List<DepartureDate>> {
        return firestore
            .collection(TRAVEL_PACKAGE_COLLECTION)
            .document(packageId)
            .collection("departures") // Querying the subcollection!
            .dataObjects()
    }

    companion object{
        private const val TRAVEL_PACKAGE_COLLECTION = "travel_packages"
    }
}