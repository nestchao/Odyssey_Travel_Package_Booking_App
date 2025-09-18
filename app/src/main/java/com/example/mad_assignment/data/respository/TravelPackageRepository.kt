package com.example.mad_assignment.data.repository
import com.example.mad_assignment.data.datasource.TravelPackageDataSource
import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class TravelPackageRepository @Inject constructor(
    private val travelPackageDataSource: TravelPackageDataSource
) {
    suspend fun getTravelPackages(): List<TravelPackage> {
        val result = travelPackageDataSource.getTravelPackages()
        return result.getOrElse { emptyList() }
    }

    suspend fun getTravelPackage(packageId: String): TravelPackage? {
        val result = travelPackageDataSource.getPackageById(packageId)
        return result.getOrNull()
    }

    suspend fun createPackage(newPackage: TravelPackage): String? {
        val result = travelPackageDataSource.createPackage(newPackage)
        return result.getOrNull()
    }

    suspend fun deletePackage(packageId: String) {
        travelPackageDataSource.deletePackage(packageId)
    }

    suspend fun getTripsByIds(ids: List<String>): List<Trip> {
        val result = travelPackageDataSource.getTripsByIds(ids)
        return result.getOrElse {
            emptyList()
        }
    }

    suspend fun resolveTripsForPackage(travelPackage: TravelPackage): Map<Int, List<Trip>> {
        val allTripIds = travelPackage.itineraries.flatMap { it.tripIds }.distinct()

        if (allTripIds.isEmpty()) {
            return emptyMap()
        }

        val trips = getTripsByIds(allTripIds)
        if (trips.isEmpty()) {
            return emptyMap()
        }

        val tripMap = trips.associateBy { it.tripId }

        return travelPackage.itineraries.associate { itineraryItem ->
            val tripsForDay = itineraryItem.tripIds.mapNotNull { tripId ->
                tripMap[tripId]
            }
            itineraryItem.day to tripsForDay
        }
    }

    suspend fun getDepartureDates(packageId: String): Result<List<DepartureDate>> {
        return travelPackageDataSource.getDepartureDatesForPackage(packageId)
    }
}