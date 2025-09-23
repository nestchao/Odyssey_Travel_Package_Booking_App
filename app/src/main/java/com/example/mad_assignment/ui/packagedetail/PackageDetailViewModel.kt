package com.example.mad_assignment.ui.packagedetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.repository.TravelPackageRepository
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
    // TODO: import cart repository here
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageId: String? = savedStateHandle.get<String>("packageId")

    private val _uiState = MutableStateFlow<PackageDetailUiState>(PackageDetailUiState.Loading)
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    init {
        Log.d("PackageDetailVM", "Initializing with packageId: $packageId")
        if (packageId != null) {
            loadPackageAndRelatedData()
        } else {
            Log.e("PackageDetailVM", "Package ID is null")
            _uiState.value = PackageDetailUiState.Error("Package ID not provided")
        }
    }

    private fun loadPackageAndRelatedData() {
        viewModelScope.launch {
            _uiState.value = PackageDetailUiState.Loading

            try {
                Log.d("PackageDetailVM", "Loading package details for ID: $packageId")

                val packageDetailData = packageRepository.getPackageWithImages(packageId!!)
                if (packageDetailData == null) {
                    Log.e("PackageDetailVM", "Package not found for ID: $packageId")
                    _uiState.value = PackageDetailUiState.Error("Package not found.")
                    return@launch
                }

                Log.d("PackageDetailVM", "Package loaded: ${packageDetailData.travelPackage.packageName}")

                coroutineScope {
                    val tripsDeferred = async {
                        try {
                            Log.d("PackageDetailVM", "Loading trips for package")
                            packageRepository.resolveTripsForPackage(packageDetailData.travelPackage)
                        } catch (e: Exception) {
                            Log.e("PackageDetailVM", "Error loading trips", e)
                            emptyMap()
                        }
                    }

                    val departuresDeferred = async {
                        try {
                            Log.d("PackageDetailVM", "Loading departure dates for package")
                            packageRepository.getDepartureDates(packageId)
                        } catch (e: Exception) {
                            Log.e("PackageDetailVM", "Error loading departures", e)
                            // Return the package options from the travel package itself as fallback
                            packageDetailData.travelPackage.packageOption
                        }
                    }

                    val itineraryTrips = tripsDeferred.await()
                    val departures = departuresDeferred.await()

                    Log.d("PackageDetailVM", "Loaded ${departures.size} departure dates")

                    // Initialize pax counts with pricing categories set to 0
                    val initialPaxCounts = packageDetailData.travelPackage.pricing.keys.associateWith { 0 }

                    _uiState.value = PackageDetailUiState.Success(
                        packageDetail = packageDetailData,
                        itineraryTrips = itineraryTrips,
                        departures = departures,
                        paxCounts = initialPaxCounts
                    )
                }
            } catch (e: Exception) {
                Log.e("PackageDetailVM", "Error loading package details", e)
                _uiState.value = PackageDetailUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun selectDeparture(departure: DepartureAndEndTime) {
        _uiState.update { currentState ->
            if (currentState is PackageDetailUiState.Success) {
                Log.d("PackageDetailVM", "Selected departure: ${departure.id}")
                currentState.copy(selectedDeparture = departure)
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
                Log.d("PackageDetailVM", "Updated pax count for $category: $newCount")

                currentState.copy(paxCounts = currentPaxCounts)
            } else {
                currentState
            }
        }
    }

    /*
     TODO: add function to pass the value to cart
     fun addToCart(){
        val newCartItem = CartItem(
            packageId = packageId,
            departureDate = (uiState.value as? PackageDetailUiState.Success)?.selectedDeparture,
            paxCounts = (uiState.value as? PackageDetailUiState.Success)?.paxCounts ?: emptyMap(),
            totalPrice = (uiState.value as? PackageDetailUiState.Success)?.totalPrice ?: 0.0
        )
        // Add to cart repository
        // get userId from current user and cartId from cartRepository.getCartByUserId(userId)
        cartRepository.addItemToCart(userId, cartId, newCartItem)
     }
     */
}