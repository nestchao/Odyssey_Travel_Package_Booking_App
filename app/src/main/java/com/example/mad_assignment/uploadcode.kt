package com.example.mad_assignment.util

import android.util.Log
import com.example.mad_assignment.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.Date

suspend fun uploadSampleTravelPackage(firestore: FirebaseFirestore) {
    Log.d("Uploader", "Starting to create a sample package...")

    val newPackage = TravelPackage(
        packageName = "Malaysian Rainforest Adventure",
        packageDescription = "Explore the ancient Taman Negara rainforest, home to incredible biodiversity. A true adventure for nature lovers.",
        durationDays = 5,
        status = PackageStatus.AVAILABLE,
        isFeatured = true,
        imageUrls = listOf(
            "https://images.unsplash.com/photo-1503185912284-527168116219",
            "https://images.unsplash.com/photo-1528181304800-259b08848526"
        ),
        pricing = mapOf(
            "adult" to 950.00,
            "child" to 500.00
        ),
        itinerary = listOf(
            Trip(
                tripName = "Day 1: Canopy Walk & Night Safari",
                description = "Walk across the world's longest canopy walkway and spot nocturnal animals on a guided night walk.",
                location = Location(
                    name = "Taman Negara National Park",
                    geoPoint = GeoPoint(4.3833, 102.4000)
                )
            ),
            Trip(
                tripName = "Day 2: Jungle Trekking & River Cruise",
                description = "Trek to the Teresek Hill for a panoramic view and enjoy a relaxing boat cruise along the Tembeling River.",
                location = Location(
                    name = "Tembeling River",
                    geoPoint = GeoPoint(4.3850, 102.4150)
                )
            )
        )
    )

    try {
        val packageDocumentRef = firestore.collection("travel_packages").add(newPackage).await()
        val newPackageId = packageDocumentRef.id
        Log.d("Uploader", "Successfully created main package with ID: $newPackageId")

        val departure1 = DepartureDate(
            startDate = Timestamp(Date(1727827200000L)), // Oct 2, 2024
            endDate = Timestamp(Date(1728172800000L)),   // Oct 6, 2024
            maxCapacity = 20,
            currentBookings = 5,
            status = "AVAILABLE"
        )
        val departure2 = DepartureDate(
            startDate = Timestamp(Date(1728432000000L)), // Oct 8, 2024
            endDate = Timestamp(Date(1728777600000L)),   // Oct 12, 2024
            maxCapacity = 20,
            currentBookings = 20,
            status = "SOLD_OUT"
        )

        val departuresSubcollection = firestore.collection("travel_packages").document(newPackageId).collection("departures")
        departuresSubcollection.add(departure1).await()
        departuresSubcollection.add(departure2).await()

        Log.d("Uploader", "Successfully added departure dates. Upload complete!")

    } catch (e: Exception) {
        Log.e("Uploader", "Error uploading package", e)
    }
}