// Create in the same package: com.example.mad_assignment.ui.managetrip
package com.example.mad_assignment.ui.managetrip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Trip
import com.example.mad_assignment.data.repository.TripRepository
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageTripViewModel @Inject constructor(
    private val repository: TripRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val tripId: String? = savedStateHandle.get("tripId")

    private val _uiState = MutableStateFlow(ManageTripUiState())
    val uiState: StateFlow<ManageTripUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        if (tripId != null) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                try {
                    val trip = repository.getTripsByIds(listOf(tripId)).firstOrNull()
                    if (trip != null) {
                        _uiState.update {
                            it.copy(
                                tripId = trip.tripId,
                                tripName = trip.tripName,
                                latitude = trip.geoPoint?.latitude?.toString() ?: "",
                                longitude = trip.geoPoint?.longitude?.toString() ?: "",
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Trip not found.") }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load trip.") }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTripNameChange(name: String) {
        _uiState.update { it.copy(tripName = name) }
    }

    fun onLatitudeChange(lat: String) {
        _uiState.update { it.copy(latitude = lat) }
    }

    fun onLongitudeChange(lon: String) {
        _uiState.update { it.copy(longitude = lon) }
    }

    fun saveTrip() = viewModelScope.launch {
        if (!validate()) {
            return@launch
        }
        _uiState.update { it.copy(isSaving = true) }

        try {
            val currentState = _uiState.value
            val geoPoint = if (currentState.latitude.isNotBlank() && currentState.longitude.isNotBlank()) {
                GeoPoint(currentState.latitude.toDouble(), currentState.longitude.toDouble())
            } else {
                null
            }

            val tripToSave = Trip(
                tripId = currentState.tripId ?: "",
                tripName = currentState.tripName.trim(),
                geoPoint = geoPoint
            )

            if (currentState.isEditing) {
                repository.updateTrip(tripToSave)
            } else {
                repository.addTrip(tripToSave)
            }
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }

        } catch (e: Exception) {
            _uiState.update { it.copy(isSaving = false, error = e.message ?: "An error occurred.") }
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        val errors = mutableMapOf<String, String?>()

        if (state.tripName.isBlank()) {
            errors["tripName"] = "Trip name cannot be empty."
        }

        val lat = state.latitude.toDoubleOrNull()
        val lon = state.longitude.toDoubleOrNull()

        if (state.latitude.isNotBlank() && (lat == null || lat !in -90.0..90.0)) {
            errors["latitude"] = "Invalid latitude (-90 to 90)."
        }
        if (state.longitude.isNotBlank() && (lon == null || lon !in -180.0..180.0)) {
            errors["longitude"] = "Invalid longitude (-180 to 180)."
        }

        if ((state.latitude.isNotBlank() && state.longitude.isBlank()) || (state.latitude.isBlank() && state.longitude.isNotBlank())) {
            errors["location"] = "Both latitude and longitude must be provided, or both left empty."
            if (state.latitude.isBlank()) errors["latitude"] = "Required if longitude is present."
            if (state.longitude.isBlank()) errors["longitude"] = "Required if latitude is present."
        }


        _uiState.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}