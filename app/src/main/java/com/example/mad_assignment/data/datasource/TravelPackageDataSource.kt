// com.example.mad_assignment.data.datasource/TravelPackageDataSource.kt
package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.model.PackageImage
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelPackageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getAllTravelPackages(): Flow<List<TravelPackage>> {
        return firestore.collection(PACKAGES_COLLECTION)
            .whereEqualTo("deletedAt", null)
            .dataObjects()
    }

    suspend fun getPackageById(packageId: String): TravelPackage? {
        return firestore.collection(PACKAGES_COLLECTION)
            .document(packageId)
            .get()
            .await()
            .toObject()
    }

    suspend fun getDepartureDatesForPackage(packageId: String): List<DepartureAndEndTime> {
        val document = firestore.collection(PACKAGES_COLLECTION)
            .document(packageId)
            .get()
            .await()
        return document.toObject<TravelPackage>()?.packageOption ?: emptyList()
    }

    suspend fun addTravelPackage(travelPackage: TravelPackage): String {
        val packageDocRef = firestore.collection(PACKAGES_COLLECTION).document()
        val finalPackage = travelPackage.copy(packageId = packageDocRef.id)
        packageDocRef.set(finalPackage).await()
        return packageDocRef.id
    }

    suspend fun softDeletePackage(packageId: String) {
        val packageDocRef = firestore.collection(PACKAGES_COLLECTION).document(packageId)
        packageDocRef.update("deletedAt", Timestamp.now()).await()
    }

    companion object {
        const val PACKAGES_COLLECTION = "packages"
    }
}