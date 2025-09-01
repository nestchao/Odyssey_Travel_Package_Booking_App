package com.example.mad_assignment.data.respository

import com.example.mad_assignment.data.datasource.TravelPackageDataSource
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TravelPackageRepository @Inject constructor(
    private val travelPackageDataSource: TravelPackageDataSource
){
    fun getTravelPackages(): Flow<List<TravelPackage>>{
        return travelPackageDataSource.getTravelPackages()
    }

    suspend fun getTravelPackage(packageId: String): TravelPackage?{
        return travelPackageDataSource.getTravelPackage(packageId)
    }

    suspend fun create(newPackage: TravelPackage): String{
        return travelPackageDataSource.create(newPackage)
    }

    suspend fun delete(packageId: String){
        travelPackageDataSource.delete(packageId)
    }

    suspend fun getTripsByIds(ids: List<String>): List<Trip> {
        return travelPackageDataSource.getTripsByIds(ids)
    }
    fun getDepartureDates(packageId: String) = travelPackageDataSource.getDepartureDates(packageId)
}