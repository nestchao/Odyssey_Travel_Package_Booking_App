package com.example.mad_assignment.data.repository

import com.example.mad_assignment.data.model.RecentlyViewedItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentlyViewedRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getRecentlyViewedCollection(userId: String?) =
        firestore.collection("users").document(userId.toString()).collection("recently_viewed")

    suspend fun addToRecentlyViewed(userId: String?, packageId: String) {
        // Remove if already exists to avoid duplicates
        val existingQuery = getRecentlyViewedCollection(userId)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()

        existingQuery.documents.forEach { it.reference.delete().await() }

        // Add new entry
        val docRef = getRecentlyViewedCollection(userId).document()
        val recentlyViewedItem = RecentlyViewedItem(
            id = docRef.id,
            packageId = packageId
        )
        docRef.set(recentlyViewedItem).await()

        // Limit to recent 20 items
        val allItems = getRecentlyViewedCollection(userId)
            .orderBy("viewedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()

        if (allItems.size() > 20) {
            val itemsToDelete = allItems.documents.subList(20, allItems.size())
            itemsToDelete.forEach { it.reference.delete().await() }
        }
    }

    suspend fun getRecentlyViewed(userId: String?): List<RecentlyViewedItem> {
        val snapshot = getRecentlyViewedCollection(userId)
            .orderBy("viewedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(RecentlyViewedItem::class.java)
    }

    suspend fun clearRecentlyViewed(userId: String?) {
        val snapshot = getRecentlyViewedCollection(userId).get().await()

        // Delete all documents in batches to avoid hitting Firestore limits
        val batch = firestore.batch()
        snapshot.documents.forEach { document ->
            batch.delete(document.reference)
        }
        batch.commit().await()
    }

    suspend fun removeFromRecentlyViewed(userId: String, packageId: String) {
        val query = getRecentlyViewedCollection(userId)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()

        query.documents.forEach { it.reference.delete().await() }
    }

    suspend fun getRecentlyViewedCount(userId: String): Int {
        val snapshot = getRecentlyViewedCollection(userId).get().await()
        return snapshot.size()
    }
}