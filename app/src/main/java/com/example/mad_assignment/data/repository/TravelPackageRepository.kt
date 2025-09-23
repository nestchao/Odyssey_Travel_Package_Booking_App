package com.example.mad_assignment.data.repository

import android.util.Log
import com.example.mad_assignment.data.datasource.TravelPackageDataSource
import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.model.PackageImage
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import com.example.mad_assignment.ui.home.TravelPackageWithImages
import com.example.mad_assignment.ui.packagedetail.PackageDetailData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch

class TravelPackageRepository @Inject constructor(
    private val dataSource: TravelPackageDataSource
) {
    fun getTravelPackages(): Flow<List<TravelPackage>> {
        return dataSource.getAllTravelPackages()
    }

    fun getImagesForPackage(packageId: String): Flow<List<PackageImage>> {
        return dataSource.getImagesForPackage(packageId)
    }

    suspend fun getTravelPackage(packageId: String): TravelPackage? {
        return dataSource.getPackageById(packageId)
    }

    suspend fun createPackageWithImages(newPackage: TravelPackage, images: List<PackageImage>): String {
        return dataSource.addTravelPackageWithImages(newPackage, images)
    }

    suspend fun deletePackage(packageId: String) {
        dataSource.softDeletePackageAndImages(packageId)
    }

    suspend fun getTripsByIds(ids: List<String>): List<Trip> {
        return dataSource.getTripsByIds(ids)
    }

    suspend fun resolveTripsForPackage(travelPackage: TravelPackage): Map<Int, List<Trip>> {
        val allTripIds = travelPackage.itineraries
            .map { it.tripId }
            .filter { it.isNotBlank() }
            .distinct()

        if (allTripIds.isEmpty()) {
            return emptyMap()
        }

        val trips = getTripsByIds(allTripIds)
        if (trips.isEmpty()) {
            return emptyMap()
        }

        val tripMap = trips.associateBy { it.tripId }

        return travelPackage.itineraries
            .groupBy { it.day }
            .mapValues { (_, itemsForDay) ->
                itemsForDay.mapNotNull { itineraryItem ->
                    tripMap[itineraryItem.tripId]
                }
            }
    }

    suspend fun getDepartureDates(packageId: String): List<DepartureAndEndTime> {
        return dataSource.getDepartureDatesForPackage(packageId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTravelPackagesWithImages(): Flow<List<TravelPackageWithImages>> {
        return dataSource.getAllTravelPackages()
            .flatMapLatest { packages ->
                Log.d("Repository", "Got ${packages.size} packages")
                if (packages.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val imageFlows: List<Flow<List<PackageImage>>> = packages.map { pkg ->
                        dataSource.getImagesForPackage(pkg.packageId)
                            .catch { e ->
                                Log.e("Repository", "Error fetching images for package ${pkg.packageId}", e)
                                emit(emptyList()) // Emit empty list if images fail to load
                            }
                    }

                    combine(imageFlows) { imagesArray: Array<List<PackageImage>> ->
                        packages.zip(imagesArray.toList()) { travelPackage, images ->
                            TravelPackageWithImages(
                                travelPackage = travelPackage,
                                images = images
                            )
                        }
                    }
                }
            }
            .catch { e ->
                Log.e("Repository", "Error in getTravelPackagesWithImages", e)
                emit(emptyList())
            }
    }

    suspend fun getPackageWithImages(packageId: String): PackageDetailData? {
        return try {
            Log.d("Repository", "Getting package with images for ID: $packageId")
            val travelPackage = dataSource.getPackageById(packageId)
            if (travelPackage == null) {
                Log.e("Repository", "Travel package not found for ID: $packageId")
                return null
            }

            Log.d("Repository", "Package found: ${travelPackage.packageName}, getting images...")
            val images = try {
                dataSource.getImagesForPackage(packageId).first()
            } catch (e: Exception) {
                Log.e("Repository", "Error getting images for package $packageId", e)
                emptyList()
            }

            Log.d("Repository", "Found ${images.size} images for package")
            PackageDetailData(
                travelPackage = travelPackage,
                images = images
            )
        } catch (e: Exception) {
            Log.e("Repository", "Error in getPackageWithImages for packageId: $packageId", e)
            null
        }
    }
}