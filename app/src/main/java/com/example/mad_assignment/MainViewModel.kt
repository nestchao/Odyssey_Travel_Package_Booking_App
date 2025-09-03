package com.example.mad_assignment.util

import android.util.Log
import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.ItineraryItem
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

class FirebaseSeeder(private val firestore: FirebaseFirestore) {

    private val packagesCollection = firestore.collection("packages")
    private val tripsCollection = firestore.collection("trips")
    private val departuresCollection = firestore.collection("departureDates")

    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val snapshot = packagesCollection.limit(1).get().await()
        if (!snapshot.isEmpty) {
            Log.d("FirebaseSeeder", "Database already contains data. Seeding skipped.")
            return@withContext
        }

        Log.d("FirebaseSeeder", "Starting database seed...")
        try {
            val tripIds = seedTrips()
            Log.d("FirebaseSeeder", "Successfully seeded ${tripIds.size} trips.")

            val packageIds = seedPackages(tripIds)
            Log.d("FirebaseSeeder", "Successfully seeded ${packageIds.size} packages.")

            seedDepartureDates(packageIds)
            Log.d("FirebaseSeeder", "Successfully seeded departure dates.")

            Log.d("FirebaseSeeder", "DATABASE SEEDING COMPLETED SUCCESSFULLY!")
        } catch (e: Exception) {
            Log.e("FirebaseSeeder", "Error seeding database", e)
        }
    }

    private suspend fun seedTrips(): Map<String, String> = coroutineScope {
        val tripsToCreate = mapOf(
            "eiffel" to Trip(tripName = "Eiffel Tower Summit Experience", geoPoint = GeoPoint(48.8584, 2.2945)),
            "louvre" to Trip(tripName = "Louvre Museum Guided Tour", geoPoint = GeoPoint(48.8606, 2.3376)),
            "seine" to Trip(tripName = "Seine River Evening Cruise", geoPoint = GeoPoint(48.8529, 2.3499)),
            "colosseum" to Trip(tripName = "Colosseum & Roman Forum Tour", geoPoint = GeoPoint(41.8902, 12.4922)),
            "vatican" to Trip(tripName = "Vatican City & St. Peter's Basilica", geoPoint = GeoPoint(41.9022, 12.4539)),
            "trevi" to Trip(tripName = "Trevi Fountain & Pantheon Walk", geoPoint = GeoPoint(41.9009, 12.4833)),
            "shibuya" to Trip(tripName = "Shibuya Crossing & Hachiko Statue", geoPoint = GeoPoint(35.6590, 139.7006)),
            "sensoji" to Trip(tripName = "Senso-ji Temple Visit", geoPoint = GeoPoint(35.7148, 139.7967)),
            "ghibli" to Trip(tripName = "Ghibli Museum Experience (Mitaka)", geoPoint = GeoPoint(35.7061, 139.5701))
        )

        val deferredTripIds = tripsToCreate.map { (key, trip) ->
            async {
                val docRef = tripsCollection.add(trip).await()
                key to docRef.id
            }
        }
        deferredTripIds.awaitAll().toMap()
    }

    private suspend fun seedPackages(tripIds: Map<String, String>): List<String> = coroutineScope {
        val packagesToCreate = listOf(
            TravelPackage(
                packageName = "Parisian Dream",
                packageDescription = "Experience the romance and beauty of Paris. From the iconic Eiffel Tower to the artistic treasures of the Louvre, this trip is a journey through the heart of French culture.",
                imageUrls = listOf("https://images.unsplash.com/photo-1502602898657-3e91760c0341", "https://images.unsplash.com/photo-1522093007474-d86e9bf7ba6f"),
                location = "Paris, France",
                durationDays = 5,
                pricing = mapOf("Adult" to 1200.00, "Child" to 650.00),
                itineraries = listOf(
                    ItineraryItem(day = 1, description = "Arrival and Eiffel Tower", tripIds = listOf(tripIds["eiffel"]!!)),
                    ItineraryItem(day = 2, description = "Art & History Day", tripIds = listOf(tripIds["louvre"]!!)),
                    ItineraryItem(day = 3, description = "Relaxing River Cruise", tripIds = listOf(tripIds["seine"]!!))
                )
            ),
            TravelPackage(
                packageName = "Roman Holiday",
                packageDescription = "Walk in the footsteps of emperors and gladiators. This adventure takes you through ancient Rome's most famous landmarks, combined with the charm of modern Italian life.",
                imageUrls = listOf("https://images.unsplash.com/photo-1529260830199-42c24129f196", "https://images.unsplash.com/photo-1552832230-c0197dd311b5"),
                location = "Rome, Italy",
                durationDays = 7,
                pricing = mapOf("Adult" to 1550.50, "Student" to 1300.00),
                itineraries = listOf(
                    ItineraryItem(day = 1, description = "Ancient Rome Exploration", tripIds = listOf(tripIds["colosseum"]!!)),
                    ItineraryItem(day = 2, description = "Vatican City Wonders", tripIds = listOf(tripIds["vatican"]!!)),
                    ItineraryItem(day = 4, description = "City Fountains & Squares", tripIds = listOf(tripIds["trevi"]!!))
                )
            ),
            TravelPackage(
                packageName = "Tokyo Adventure",
                packageDescription = "Dive into the vibrant culture of Tokyo, where ancient traditions meet futuristic technology. Explore bustling cityscapes, serene temples, and unique pop culture.",
                imageUrls = listOf("https://images.unsplash.com/photo-1542051841857-5f90071e7989", "https://images.unsplash.com/photo-1536098561742-ca998e48cbcc"),
                location = "Tokyo, Japan",
                durationDays = 8,
                pricing = mapOf("Adult" to 2200.00),
                itineraries = listOf(
                    ItineraryItem(day = 1, description = "Arrival in Shinjuku", tripIds = emptyList()),
                    ItineraryItem(day = 2, description = "Bustling Shibuya", tripIds = listOf(tripIds["shibuya"]!!)),
                    ItineraryItem(day = 3, description = "Asakusa Traditions", tripIds = listOf(tripIds["sensoji"]!!)),
                    ItineraryItem(day = 5, description = "Day trip to Mitaka", tripIds = listOf(tripIds["ghibli"]!!))
                )
            )
        )

        // CORRECTED BLOCK
        val deferredPackageIds = packagesToCreate.map { travelPackage ->
            async {
                val docRef = packagesCollection.add(travelPackage).await()
                // Return the new ID
                docRef.id
            }
        }
        // awaitAll will return a List<String>
        deferredPackageIds.awaitAll()
    }

    private suspend fun seedDepartureDates(packageIds: List<String>) {
        val batch = firestore.batch()
        val calendar = Calendar.getInstance()

        packageIds.forEach { packageId ->
            for (i in 1..3) {
                calendar.time = Date()
                calendar.add(Calendar.MONTH, i)
                val futureDate = calendar.time

                val newDeparture = DepartureDate(
                    packageId = packageId,
                    startDate = Timestamp(futureDate),
                    capacity = (10..30).random()
                )
                val docRef = departuresCollection.document()
                batch.set(docRef, newDeparture)
            }
        }
        batch.commit().await()
    }
}