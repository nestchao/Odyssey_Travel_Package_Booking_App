package com.example.mad_assignment.data.repository

import com.example.mad_assignment.data.datasource.TripDataSource
import com.example.mad_assignment.data.model.Trip
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val tripDataSource: TripDataSource
) {
    fun getAllTrips(): Flow<List<Trip>> {
        return tripDataSource.getAllTrips()
    }

    suspend fun getTripsByIds(ids: List<String>): List<Trip> {
        return tripDataSource.getTripsByIds(ids)
    }

    suspend fun addTrip(trip: Trip) {
        tripDataSource.addTrip(trip)
    }

    suspend fun deleteTrip(tripId: String) {
        tripDataSource.softDeleteTrip(tripId)
    }

    suspend fun updateTrip(trip: Trip) {
        tripDataSource.updateTrip(trip)
    }
}