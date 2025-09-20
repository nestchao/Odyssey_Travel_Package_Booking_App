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

    fun getImagesForPackage(packageId: String): Flow<List<PackageImage>> {
        return firestore.collection(PACKAGE_IMAGES_COLLECTION)
            .whereEqualTo("packageId", packageId)
            .whereEqualTo("deletedAt", null)
            .orderBy("order", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun getPackageById(packageId: String): TravelPackage? {
        return firestore.collection(PACKAGES_COLLECTION)
            .document(packageId)
            .get()
            .await()
            .toObject()
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

    suspend fun getDepartureDatesForPackage(packageId: String): List<DepartureAndEndTime> {
        val document = firestore.collection(PACKAGES_COLLECTION)
            .document(packageId)
            .get()
            .await()

        return document.toObject<TravelPackage>()?.packageOption ?: emptyList()
    }

    suspend fun addTravelPackageWithImages(
        travelPackage: TravelPackage,
        images: List<PackageImage>
    ): String {
        val packageDocRef = firestore.collection(PACKAGES_COLLECTION).document()
        val packageId = packageDocRef.id

        val imageDocIds = mutableListOf<String>()
        val batch = firestore.batch()

        images.forEach { image ->
            val imageDocRef = firestore.collection(PACKAGE_IMAGES_COLLECTION).document()
            imageDocIds.add(imageDocRef.id)

            val finalImage = image.copy(imageId = imageDocRef.id, packageId = packageId)
            batch.set(imageDocRef, finalImage)
        }

        val finalPackage = travelPackage.copy(
            packageId = packageId,
            imageDocsId = imageDocIds
        )
        batch.set(packageDocRef, finalPackage)
        batch.commit().await()
        return packageId
    }

    suspend fun softDeletePackageAndImages(packageId: String) {
        val deleteTimestamp = Timestamp.now()
        val batch = firestore.batch()

        val imagesQuery = firestore.collection(PACKAGE_IMAGES_COLLECTION)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()

        imagesQuery.documents.forEach { doc ->
            batch.update(doc.reference, "deletedAt", deleteTimestamp)
        }

        val packageDocRef = firestore.collection(PACKAGES_COLLECTION).document(packageId)
        batch.update(packageDocRef, "deletedAt", deleteTimestamp)

        batch.commit().await()
    }

    companion object {
        private const val PACKAGES_COLLECTION = "packages"
        private const val TRIPS_COLLECTION = "trips"
        private const val PACKAGE_IMAGES_COLLECTION = "packageImages"
    }
}