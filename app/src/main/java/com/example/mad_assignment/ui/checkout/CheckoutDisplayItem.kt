package com.example.mad_assignment.ui.checkout

import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.util.base64ToDataUri
import com.google.firebase.Timestamp

// A unified data class to represent items on the checkout screen
data class CheckoutDisplayItem(
    val packageId: String,
    val packageName: String,
    val location: String,
    val imageUri: String?,
    val departureDate: Timestamp?,
    val paxInfo: String,
    val price: Double
) {
    companion object {
        // Factory function to create an item from a "Direct Buy"
        fun from(
            pkgWithImages: TravelPackageWithImages,
            paxCounts: Map<String, Int>,
            totalPrice: Double,
            departureId: String?
        ): CheckoutDisplayItem {
            val travelPackage = pkgWithImages.travelPackage
            val selectedDeparture = travelPackage.packageOption.find { it.id == departureId }

            val paxDetails = mutableListOf<String>()
            // FIX #1: Use the correct, case-sensitive keys ("Adult", "Child")
            paxCounts["Adult"]?.takeIf { it > 0 }?.let { paxDetails.add("$it Adult(s)") }
            paxCounts["Child"]?.takeIf { it > 0 }?.let { paxDetails.add("$it Child(ren)") }

            return CheckoutDisplayItem(
                packageId = travelPackage.packageId,
                packageName = travelPackage.packageName,
                location = travelPackage.location,
                imageUri = base64ToDataUri(pkgWithImages.images.firstOrNull()?.base64Data),
                departureDate = selectedDeparture?.startDate,
                paxInfo = paxDetails.joinToString(", "),
                price = totalPrice
            )
        }

        // Factory function to create an item from a CartItem
        fun from(
            pkgWithImages: TravelPackageWithImages,
            cartItem: CartItem
        ): CheckoutDisplayItem {
            val travelPackage = pkgWithImages.travelPackage

            val paxDetails = mutableListOf<String>()
            // FIX #2: Apply the same logic here for consistency
            cartItem.noOfAdults.takeIf { it > 0 }?.let { paxDetails.add("$it Adult(s)") }
            cartItem.noOfChildren.takeIf { it > 0 }?.let { paxDetails.add("$it Child(ren)") }

            return CheckoutDisplayItem(
                packageId = travelPackage.packageId,
                packageName = travelPackage.packageName,
                location = travelPackage.location,
                imageUri = base64ToDataUri(pkgWithImages.images.firstOrNull()?.base64Data),
                departureDate = cartItem.startDate,
                paxInfo = paxDetails.joinToString(", "),
                price = cartItem.totalPrice
            )
        }
    }
}