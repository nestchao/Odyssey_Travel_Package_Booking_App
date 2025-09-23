package com.example.mad_assignment.data.repository

import android.util.Log
import com.example.mad_assignment.data.datasource.PackageImageDataSource
import com.example.mad_assignment.data.datasource.TravelPackageDataSource
import com.example.mad_assignment.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravelPackageRepository @Inject constructor(
    private val packageDataSource: TravelPackageDataSource,
    private val imageDataSource: PackageImageDataSource,
    private val tripRepository: TripRepository,
    private val firestore: FirebaseFirestore
) {
    fun getTravelPackages(): Flow<List<TravelPackage>> {
        return packageDataSource.getAllTravelPackages()
    }

    suspend fun getTravelPackage(packageId: String): TravelPackage? {
        return packageDataSource.getPackageById(packageId)
    }

    suspend fun getDepartureDates(packageId: String): List<DepartureAndEndTime> {
        return packageDataSource.getDepartureDatesForPackage(packageId)
    }

    fun getTravelPackagesWithImages(): Flow<List<TravelPackageWithImages>> {
        return packageDataSource.getAllTravelPackages()
            .flatMapLatest { packages ->
                if (packages.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val imageFlows: List<Flow<List<PackageImage>>> = packages.map { pkg ->
                        imageDataSource.getImagesForPackage(pkg.packageId)
                            .catch { e ->
                                Log.e("Repository", "Error images for ${pkg.packageId}", e)
                                emit(emptyList())
                            }
                    }
                    combine(imageFlows) { imagesArray ->
                        packages.zip(imagesArray.toList(), ::TravelPackageWithImages)
                    }
                }
            }
            .catch { e ->
                Log.e("Repository", "Error in getTravelPackagesWithImages", e)
                emit(emptyList())
            }
    }

    suspend fun getPackageWithImages(packageId: String): TravelPackageWithImages? {
        val travelPackage = packageDataSource.getPackageById(packageId) ?: return null
        val images = imageDataSource.getImagesForPackage(packageId).first()
        return TravelPackageWithImages(travelPackage, images)
    }

    suspend fun resolveTripsForPackage(travelPackage: TravelPackage): Map<Int, List<Trip>> {
        val allTripIds = travelPackage.itineraries.map { it.tripId }.filter { it.isNotBlank() }.distinct()
        if (allTripIds.isEmpty()) return emptyMap()

        val trips = tripRepository.getTripsByIds(allTripIds)
        if (trips.isEmpty()) return emptyMap()

        val tripMap = trips.associateBy { it.tripId }

        return travelPackage.itineraries
            .groupBy { it.day }
            .mapValues { (_, itemsForDay) ->
                itemsForDay.mapNotNull { tripMap[it.tripId] }
            }
    }

    suspend fun createPackageWithImages(newPackage: TravelPackage, images: List<PackageImage>): String {
        val batch = firestore.batch()

        val packageCollection = firestore.collection(TravelPackageDataSource.PACKAGES_COLLECTION)
        val packageDocRef = packageCollection.document()
        val packageId = packageDocRef.id

        val imageCollection = firestore.collection(PackageImageDataSource.PACKAGE_IMAGES_COLLECTION)
        val imageDocIds = mutableListOf<String>()
        val finalImages = images.map { image ->
            val imageDocRef = imageCollection.document()
            imageDocIds.add(imageDocRef.id)
            image.copy(imageId = imageDocRef.id, packageId = packageId)
        }

        finalImages.forEach { finalImage ->
            val docRef = imageCollection.document(finalImage.imageId)
            batch.set(docRef, finalImage)
        }


        val finalPackage = newPackage.copy(
            packageId = packageId,
            imageDocsId = imageDocIds,
            createdAt = Timestamp.now()
        )
        batch.set(packageDocRef, finalPackage)

        batch.commit().await()
        return packageId
    }

    suspend fun deletePackage(packageId: String) {
        val batch = firestore.batch()
        val deleteTimestamp = Timestamp.now()

        val packageDocRef = firestore.collection(TravelPackageDataSource.PACKAGES_COLLECTION).document(packageId)
        batch.update(packageDocRef, "deletedAt", deleteTimestamp)

        val imagesQuery = firestore.collection(PackageImageDataSource.PACKAGE_IMAGES_COLLECTION)
            .whereEqualTo("packageId", packageId)
            .get().await()
        imagesQuery.documents.forEach { doc ->
            batch.update(doc.reference, "deletedAt", deleteTimestamp)
        }

        batch.commit().await()
    }

    suspend fun updatePackageWithImages(
        packageToUpdate: TravelPackage,
        newImages: List<PackageImage>,
        removedImageIds: Set<String>
    ) {
        val batch = firestore.batch()
        val packageId = packageToUpdate.packageId
        if (packageId.isEmpty()) {
            throw IllegalArgumentException("Package ID cannot be empty for an update.")
        }

        val imageCollection = firestore.collection(PackageImageDataSource.PACKAGE_IMAGES_COLLECTION)

        // 1. Add new images to the batch
        val newImageDocIds = mutableListOf<String>()
        val finalNewImages = newImages.map { image ->
            val imageDocRef = imageCollection.document()
            newImageDocIds.add(imageDocRef.id)
            // Ensure the new images are linked to the existing package
            image.copy(imageId = imageDocRef.id, packageId = packageId)
        }
        finalNewImages.forEach { finalImage ->
            val docRef = imageCollection.document(finalImage.imageId)
            batch.set(docRef, finalImage)
        }

        // 2. Soft-delete removed images
        val deleteTimestamp = Timestamp.now()
        removedImageIds.forEach { imageId ->
            val docRef = imageCollection.document(imageId)
            batch.update(docRef, "deletedAt", deleteTimestamp)
        }

        // 3. Prepare to update the main package document
        val packageDocRef = firestore.collection(TravelPackageDataSource.PACKAGES_COLLECTION).document(packageId)

        // Fetch current package to get the existing list of image IDs
        val currentPackage = packageDocRef.get().await().toObject(TravelPackage::class.java)
        val existingImageIds = currentPackage?.imageDocsId ?: emptyList()

        // Calculate the final list of image IDs
        val finalImageIds = (existingImageIds - removedImageIds) + newImageDocIds

        val finalPackageData = packageToUpdate.copy(imageDocsId = finalImageIds)

        // Update the package document in the batch
        batch.set(packageDocRef, finalPackageData)

        // 4. Commit all changes atomically
        batch.commit().await()
    }
}