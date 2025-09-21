package com.example.mad_assignment.data.respository

import com.example.mad_assignment.data.model.WishlistItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishlistRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getWishlistCollection(userId: String) =
        firestore.collection("users").document(userId).collection("user_wishlists")

    suspend fun addToWishlist(userId: String, packageId: String): String {
        val docRef = getWishlistCollection(userId).document()
        val wishlistItem = WishlistItem(
            id = docRef.id,
            packageId = packageId
        )
        docRef.set(wishlistItem).await()
        return docRef.id
    }

    suspend fun removeFromWishlist(userId: String, wishlistItemId: String) {
        getWishlistCollection(userId).document(wishlistItemId).delete().await()
    }

    suspend fun getWishlist(userId: String): List<WishlistItem> {
        val snapshot = getWishlistCollection(userId)
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(WishlistItem::class.java)
    }

    suspend fun isInWishlist(userId: String, packageId: String): Boolean {
        val snapshot = getWishlistCollection(userId)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()
        return !snapshot.isEmpty
    }

    suspend fun getWishlistItemId(userId: String, packageId: String): String? {
        val snapshot = getWishlistCollection(userId)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.id
    }

    suspend fun getWishlistItemByPackageId(userId: String, packageId: String): WishlistItem? {
        val snapshot = getWishlistCollection(userId)
            .whereEqualTo("packageId", packageId)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(WishlistItem::class.java)
    }
}