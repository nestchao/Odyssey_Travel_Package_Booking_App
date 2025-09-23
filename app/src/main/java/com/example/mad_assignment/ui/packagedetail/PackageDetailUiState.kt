package com.example.mad_assignment.ui.packagedetail

import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip
import com.example.mad_assignment.data.model.TravelPackageWithImages

sealed interface PackageDetailUiState {
    object Loading : PackageDetailUiState
    data class Error(val message: String) : PackageDetailUiState
    data class Success(
        val packageDetail: TravelPackageWithImages,
        val itineraryTrips: Map<Int, List<Trip>> = emptyMap(),
        val departures: List<DepartureAndEndTime> = emptyList(),
        val selectedDeparture: DepartureAndEndTime? = null,
        val paxCounts: Map<String, Int> = emptyMap()
    ) : PackageDetailUiState {
        val totalPrice: Double
            get() {
                if (selectedDeparture == null) return 0.0
                val pricingMap = packageDetail.travelPackage.pricing
                return paxCounts.entries.sumOf { (category, count) ->
                    (pricingMap[category] ?: 0.0) * count
                }
            }
    }
}