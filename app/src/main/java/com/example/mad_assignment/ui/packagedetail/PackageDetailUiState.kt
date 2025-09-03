package com.example.mad_assignment.ui.packagedetail

import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip

/**
 * Represents the different states for the Package Detail screen.
 */
sealed interface PackageDetailUiState {
    object Loading : PackageDetailUiState
    data class Error(val message: String) : PackageDetailUiState
    data class Success(
        val travelPackage: TravelPackage,
        val itineraryTrips: Map<Int, List<Trip>> = emptyMap(),
        val departures: List<DepartureDate> = emptyList(),
        val selectedDeparture: DepartureDate? = null,
        val paxCounts: Map<String, Int> = emptyMap()
    ) : PackageDetailUiState {
        val totalPrice: Double
            get() {
                if (selectedDeparture == null) return 0.0

                val pricingMap = travelPackage.pricing
                return paxCounts.entries.sumOf { (category, count) ->
                    (pricingMap[category] ?: 0.0) * count
                }
            }
    }
}