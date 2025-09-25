package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.PackageImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.dataObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageImageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getImagesForPackage(packageId: String): Flow<List<PackageImage>> {
        return firestore.collection(PACKAGE_IMAGES_COLLECTION)
            .whereEqualTo("packageId", packageId)
            .whereEqualTo("deletedAt", null)
            .orderBy("order", Query.Direction.ASCENDING)
            .dataObjects()
    }

    suspend fun addImages(images: List<PackageImage>) {
        val batch = firestore.batch()
        images.forEach { image ->
            val imageDocRef = firestore.collection(PACKAGE_IMAGES_COLLECTION).document()
            val finalImage = image.copy(imageId = imageDocRef.id)
            batch.set(imageDocRef, finalImage)
        }
        batch.commit().await()
    }

    suspend fun softDeleteImagesForPackage(packageId: String) {
        val deleteTimestamp = Timestamp.now()
        val batch = firestore.batch()
        val imagesQuery = firestore.collection(PACKAGE_IMAGES_COLLECTION)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()

        imagesQuery.documents.forEach { doc ->
            batch.update(doc.reference, "deletedAt", deleteTimestamp)
        }
        batch.commit().await()
    }

    companion object {
        const val PACKAGE_IMAGES_COLLECTION = "packageImages"
    }
}