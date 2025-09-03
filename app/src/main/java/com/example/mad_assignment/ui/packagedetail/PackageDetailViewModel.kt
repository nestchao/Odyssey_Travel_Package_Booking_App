package com.example.mad_assignment.ui.packagedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.repository.TravelPackageRepository // Corrected package name
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackageDetailViewModel @Inject constructor(
    private val packageRepository: TravelPackageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageId: String = savedStateHandle.get<String>("packageId")!!

    // A single source of truth for the entire screen's state.
    private val _uiState = MutableStateFlow<PackageDetailUiState>(PackageDetailUiState.Loading)
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    init {
        loadPackageAndRelatedData()
    }

    private fun loadPackageAndRelatedData() {
        viewModelScope.launch {
            _uiState.value = PackageDetailUiState.Loading

            try {
                // Step 1: Fetch the main travel package.
                val travelPackage = packageRepository.getTravelPackage(packageId)
                if (travelPackage == null) {
                    _uiState.value = PackageDetailUiState.Error("Package not found.")
                    return@launch
                }

                // Step 2: Fetch related data (trips and departures) in parallel for efficiency.
                coroutineScope {
                    val tripsDeferred = async { packageRepository.resolveTripsForPackage(travelPackage) }
                    val departuresDeferred = async { packageRepository.getDepartureDates(packageId) }

                    val itineraryTrips = tripsDeferred.await()
                    val departures = departuresDeferred.await()

                    _uiState.value = PackageDetailUiState.Success(
                        travelPackage = travelPackage,
                        itineraryTrips = itineraryTrips,
                        departures = departures.getOrDefault(emptyList()) // Handle Result type from repo
                    )
                }

            } catch (e: Exception) {
                _uiState.value = PackageDetailUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun selectDeparture(departure: DepartureDate) {
        _uiState.update { currentState ->
            if (currentState is PackageDetailUiState.Success) {
                // Initialize pax counts based on the available pricing categories for the package.
                val initialPax = currentState.travelPackage.pricing.keys.associateWith { 1 }

                // Create a new Success state object by copying the old one.
                currentState.copy(
                    selectedDeparture = departure,
                    paxCounts = initialPax
                )
            } else {
                currentState
            }
        }
    }
    fun updatePaxCount(category: String, change: Int) {
        _uiState.update { currentState ->
            if (currentState is PackageDetailUiState.Success) {
                val currentPaxCounts = currentState.paxCounts.toMutableMap()
                val currentCount = currentPaxCounts[category] ?: 0
                val newCount = (currentCount + change).coerceAtLeast(0)
                currentPaxCounts[category] = newCount

                currentState.copy(paxCounts = currentPaxCounts)
            } else {
                currentState
            }
        }
    }
}