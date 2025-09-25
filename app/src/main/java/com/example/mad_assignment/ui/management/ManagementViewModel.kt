package com.example.mad_assignment.ui.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.model.Trip
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ManagementUiState {
    data class Success(
        val packages: List<TravelPackageWithImages>,
        val trips: List<Trip>
    ) : ManagementUiState
    data class Error(val message: String) : ManagementUiState
    object Loading : ManagementUiState
}

@HiltViewModel
class ManagementViewModel @Inject constructor(
    private val travelPackageRepository: TravelPackageRepository,
    private val tripRepository: TripRepository
) : ViewModel() {

    val uiState: StateFlow<ManagementUiState> = combine(
        travelPackageRepository.getTravelPackagesWithImages(),
        tripRepository.getAllTrips()
    ) { packages, trips ->
        ManagementUiState.Success(packages, trips) as ManagementUiState
    }.catch {
        emit(ManagementUiState.Error(it.message ?: "An unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ManagementUiState.Loading
    )

    fun deletePackage(packageId: String) {
        viewModelScope.launch {
            try {
                travelPackageRepository.deletePackage(packageId)
            } catch (e: Exception) {
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            try {
                tripRepository.deleteTrip(tripId)
            } catch (e: Exception) {

            }
        }
    }
}