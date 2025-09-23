package com.example.mad_assignment.util

import android.util.Log
import com.example.mad_assignment.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

object DataUploader {

    private const val TAG = "DataUploader"

    // A tiny, transparent 1x1 pixel PNG encoded in Base64.
    // This is a placeholder so Coil/AsyncImage has something to load.
    // You can replace this with actual Base64 image data.
    private const val PLACEHOLDER_IMAGE_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII="

    /**
     * Main function to seed all necessary sample data.
     * WARNING: This will clear existing 'trips', 'packages', and 'packageImages' collections.
     */
    suspend fun seedDatabase(firestore: FirebaseFirestore) {
        Log.d(TAG, "Starting database seeding...")
        try {
            // Clear existing data for a fresh start (optional, but good for testing)
            clearCollections(firestore, listOf("trips", "packages", "packageImages"))

            // 1. Create Trips first, as we need their IDs for itineraries.
            val tripsMap = uploadSampleTrips(firestore)
            Log.d(TAG, "Uploaded ${tripsMap.size} sample trips.")

            // 2. Create Travel Packages using the trip data.
            uploadKualaLumpurPackage(firestore, tripsMap)
            uploadPenangPackage(firestore, tripsMap)
            uploadLangkawiPackage(firestore, tripsMap)

            Log.d(TAG, "Database seeding completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding database", e)
        }
    }

    private suspend fun uploadSampleTrips(firestore: FirebaseFirestore): Map<String, Trip> {
        val tripsToCreate = mapOf(
            "kl_towers" to Trip(tripName = "Petronas Twin Towers Visit", geoPoint = GeoPoint(3.1578, 101.7119)),
            "kl_caves" to Trip(tripName = "Batu Caves Exploration", geoPoint = GeoPoint(3.2372, 101.6839)),
            "kl_merdeka" to Trip(tripName = "Merdeka Square Tour", geoPoint = GeoPoint(3.1477, 101.6936)),
            "png_hill" to Trip(tripName = "Penang Hill & The Habitat", geoPoint = GeoPoint(5.4244, 100.2687)),
            "png_streetart" to Trip(tripName = "Georgetown Street Art Hunt", geoPoint = GeoPoint(5.4149, 100.3385)),
            "png_kekloksi" to Trip(tripName = "Kek Lok Si Temple Visit", geoPoint = GeoPoint(5.3996, 100.2721)),
            "lgk_skybridge" to Trip(tripName = "Langkawi Sky Bridge & Cable Car", geoPoint = GeoPoint(6.3813, 99.6698)),
            "lgk_mangrove" to Trip(tripName = "Kilim Geoforest Park Mangrove Tour", geoPoint = GeoPoint(6.4167, 99.8500))
        )

        val createdTrips = mutableMapOf<String, Trip>()
        for ((key, trip) in tripsToCreate) {
            val docRef = firestore.collection("trips").add(trip).await()
            createdTrips[key] = trip.copy(tripId = docRef.id)
        }
        return createdTrips
    }

    private suspend fun uploadKualaLumpurPackage(firestore: FirebaseFirestore, trips: Map<String, Trip>) {
        val batch = firestore.batch()
        val packageDocRef = firestore.collection("packages").document()
        val packageId = packageDocRef.id

        // Create Image Docs
        val imageDocIds = (1..3).map { index ->
            val imageDocRef = firestore.collection("packageImages").document()
            val packageImage = PackageImage(
                imageId = imageDocRef.id,
                packageId = packageId,
                base64Data = PLACEHOLDER_IMAGE_BASE64,
                order = index
            )
            batch.set(imageDocRef, packageImage)
            imageDocRef.id
        }

        // Create Itinerary
        val itinerary = listOf(
            ItineraryItem(day = 1, description = "Arrival & City Center Exploration", startTime = "14:00", endTime = "18:00", tripId = trips["kl_merdeka"]!!.tripId),
            ItineraryItem(day = 2, description = "Skyscrapers and Culture", startTime = "09:00", endTime = "17:00", tripId = trips["kl_towers"]!!.tripId),
            ItineraryItem(day = 2, description = "Evening Cultural Visit", startTime = "18:00", endTime = "20:00", tripId = trips["kl_caves"]!!.tripId),
            ItineraryItem(day = 3, description = "Departure", startTime = "10:00", endTime = "12:00", tripId = "")
        )

        // Create Departure Options
        val packageOptions = listOf(
            createDepartureOption(addMonths = 1, durationDays = 3),
            createDepartureOption(addMonths = 2, durationDays = 3, capacity = 15, booked = 5)
        )

        // Create final TravelPackage object
        val klPackage = TravelPackage(
            packageId = packageId,
            packageName = "Kuala Lumpur City Explorer",
            packageDescription = "Discover the vibrant heart of Malaysia, from towering skyscrapers to sacred caves. A perfect blend of modern marvels and cultural heritage.",
            imageDocsId = imageDocIds,
            location = "Kuala Lumpur",
            durationDays = 3,
            pricing = mapOf("Adult" to 899.00, "Child" to 599.00),
            itineraries = itinerary,
            packageOption = packageOptions,
            status = TravelPackage.PackageStatus.AVAILABLE,
            createdAt = Timestamp.now()
        )

        batch.set(packageDocRef, klPackage)
        batch.commit().await()
        Log.d(TAG, "Uploaded Kuala Lumpur package with ID: $packageId")
    }

    private suspend fun uploadPenangPackage(firestore: FirebaseFirestore, trips: Map<String, Trip>) {
        val batch = firestore.batch()
        val packageDocRef = firestore.collection("packages").document()
        val packageId = packageDocRef.id

        val imageDocIds = (1..3).map { index ->
            val imageDocRef = firestore.collection("packageImages").document()
            batch.set(imageDocRef, PackageImage(imageId = imageDocRef.id, packageId = packageId, base64Data = PLACEHOLDER_IMAGE_BASE64, order = index))
            imageDocRef.id
        }

        val itinerary = listOf(
            ItineraryItem(day = 1, description = "Arrival & Georgetown Discovery", tripId = trips["png_streetart"]!!.tripId),
            ItineraryItem(day = 2, description = "Temple Visit & Hilltop Views", tripId = trips["png_kekloksi"]!!.tripId),
            ItineraryItem(day = 2, description = "Afternoon Nature Escape", tripId = trips["png_hill"]!!.tripId),
            ItineraryItem(day = 3, description = "Free & Easy, Departure")
        )

        val penangPackage = TravelPackage(
            packageId = packageId,
            packageName = "Penang Foodie & Heritage Trail",
            packageDescription = "Experience the Pearl of the Orient, a UNESCO World Heritage Site famed for its colonial architecture, vibrant street art, and legendary street food.",
            imageDocsId = imageDocIds,
            location = "Penang",
            durationDays = 3,
            pricing = mapOf("Adult" to 1150.00, "Child" to 750.00),
            itineraries = itinerary,
            packageOption = listOf(createDepartureOption(1, 3), createDepartureOption(2, 3, 20, 18)),
            status = TravelPackage.PackageStatus.AVAILABLE,
            createdAt = Timestamp.now()
        )
        batch.set(packageDocRef, penangPackage)
        batch.commit().await()
        Log.d(TAG, "Uploaded Penang package with ID: $packageId")
    }

    private suspend fun uploadLangkawiPackage(firestore: FirebaseFirestore, trips: Map<String, Trip>) {
        val batch = firestore.batch()
        val packageDocRef = firestore.collection("packages").document()
        val packageId = packageDocRef.id

        val imageDocIds = (1..2).map { index ->
            val imageDocRef = firestore.collection("packageImages").document()
            batch.set(imageDocRef, PackageImage(imageId = imageDocRef.id, packageId = packageId, base64Data = PLACEHOLDER_IMAGE_BASE64, order = index))
            imageDocRef.id
        }

        val itinerary = listOf(
            ItineraryItem(day = 1, description = "Arrival and Beach Relaxation"),
            ItineraryItem(day = 2, description = "Sky Bridge and Geoforest Adventure", tripId = trips["lgk_skybridge"]!!.tripId),
            ItineraryItem(day = 2, description = "Mangrove Tour", tripId = trips["lgk_mangrove"]!!.tripId),
            ItineraryItem(day = 3, description = "Departure")
        )

        val langkawiPackage = TravelPackage(
            packageId = packageId,
            packageName = "Langkawi Island Paradise",
            packageDescription = "Escape to the Jewel of Kedah, an archipelago of 99 islands offering pristine beaches, lush rainforests, and breathtaking views. A true tropical getaway.",
            imageDocsId = imageDocIds,
            location = "Langkawi",
            durationDays = 3,
            pricing = mapOf("Adult" to 1499.00, "Child" to 999.00),
            itineraries = itinerary,
            packageOption = listOf(createDepartureOption(2, 3, 10, 10)), // This one is sold out
            status = TravelPackage.PackageStatus.SOLD_OUT,
            createdAt = Timestamp.now()
        )
        batch.set(packageDocRef, langkawiPackage)
        batch.commit().await()
        Log.d(TAG, "Uploaded Langkawi package with ID: $packageId")
    }

    // Helper to create departure dates in the future
    private fun createDepartureOption(addMonths: Int, durationDays: Int, capacity: Int = 20, booked: Int = 0): DepartureAndEndTime {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.MONTH, addMonths)
        val startDate = Timestamp(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, durationDays - 1)
        val endDate = Timestamp(calendar.time)

        return DepartureAndEndTime(
            startDate = startDate,
            endDate = endDate,
            capacity = capacity,
            bookedCount = booked
        )
    }

    // Helper to clear collections before seeding
    private suspend fun clearCollections(firestore: FirebaseFirestore, collectionNames: List<String>) {
        collectionNames.forEach { collectionName ->
            val snapshot = firestore.collection(collectionName).get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Log.d(TAG, "Cleared collection: $collectionName")
        }
    }
}