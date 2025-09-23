package com.example.mad_assignment.ui.managetravelpackage

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.*
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.TripRepository
import com.example.mad_assignment.util.uriToBase64
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ManageTravelPackageViewModel @Inject constructor(
    private val repository: TravelPackageRepository,
    private val tripRepository: TripRepository,
    private val savedStateHandle: SavedStateHandle,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageTravelPackageUiState())
    val uiState: StateFlow<ManageTravelPackageUiState> = _uiState.asStateFlow()

    // ... (init, loadAvailableTrips, loadInitialData are unchanged)
    private val packageId: String? = savedStateHandle.get("packageId")

    init {
        loadAvailableTrips()
        loadInitialData()
    }
    private fun loadAvailableTrips() {
        viewModelScope.launch {
            tripRepository.getAllTrips()
                .catch { e ->
                    _uiState.update { it.copy(error = "Failed to load trips: ${e.message}") }
                }
                .collect { trips ->
                    _uiState.update { it.copy(availableTrips = trips) }
                }
        }
    }
    private fun loadInitialData() {
        if (packageId != null) {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val packageWithImages = repository.getPackageWithImages(packageId)
                if (packageWithImages != null) {
                    val pkg = packageWithImages.travelPackage
                    _uiState.update {
                        it.copy(
                            packageId = pkg.packageId,
                            packageName = pkg.packageName,
                            packageDescription = pkg.packageDescription,
                            location = pkg.location,
                            durationDays = pkg.durationDays.toString(),
                            pricing = pkg.pricing.mapValues { entry -> "%.2f".format(entry.value) },
                            itineraries = pkg.itineraries,
                            packageOptions = pkg.packageOption,
                            createdAt = pkg.createdAt,
                            initialImages = packageWithImages.images,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Package not found.") }
                }
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }


    // --- VALIDATION LOGIC (UPDATED) ---
    private fun validate(): Boolean {
        val state = _uiState.value
        val errors = mutableMapOf<String, String?>()

        if (state.packageName.isBlank()) {
            errors["packageName"] = "Package name cannot be empty."
        }
        if (state.packageDescription.isBlank()) {
            errors["packageDescription"] = "Description cannot be empty."
        }
        if (state.location.isBlank()) {
            errors["location"] = "Location cannot be empty."
        }
        if (state.durationDays.isBlank() || state.durationDays.toIntOrNull() == 0) {
            errors["durationDays"] = "Duration must be a number greater than 0."
        }
        if (state.pricing["Adult"].isNullOrBlank()) {
            errors["price_adult"] = "Adult price is required."
        }
        if (state.displayedImages.isEmpty()) {
            errors["images"] = "At least one image is required."
        }
        if (state.packageOptions.isEmpty()) {
            errors["departureDates"] = "At least one departure date is required."
        }

        // --- ENTIRE ITINERARY VALIDATION BLOCK - REVISED ---
        // First, check if duration is set. If it is, the itinerary cannot be empty.
        if ((state.durationDays.toIntOrNull() ?: 0) > 0 && state.itineraries.isEmpty()) {
            errors["itinerary"] = "Please add at least one activity to the itinerary."
        } else {
            // If the itinerary is not empty, then check if any items are incomplete.
            val hasIncompleteItinerary = state.itineraries.any { it.startTime.isBlank() || it.endTime.isBlank() }
            if (hasIncompleteItinerary) {
                errors["itinerary"] = "Please complete the details (start and end time) for all activities."
            } else {
                // If all items are complete, check for time overlaps.
                val overlappingError = validateItineraryTimes(state.itineraries)
                if (overlappingError != null) {
                    errors["itinerary"] = overlappingError
                }
            }
        }


        _uiState.update { it.copy(validationErrors = errors) }
        return errors.isEmpty()
    }

    private fun timeToMinutes(time: String): Int? {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(time)
            val calendar = Calendar.getInstance().apply { timeInMillis = date.time }
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        } catch (e: Exception) {
            null
        }
    }

    private fun validateItineraryTimes(itineraries: List<ItineraryItem>): String? {
        val groupedByDay = itineraries.groupBy { it.day }

        for ((day, items) in groupedByDay) {
            val timeRanges = items.mapNotNull {
                val start = timeToMinutes(it.startTime)
                val end = timeToMinutes(it.endTime)
                if (start != null && end != null) {
                    if (end <= start) return@validateItineraryTimes "Day $day: End time must be after start time."
                    Pair(start, end)
                } else {
                    null // Ignore items with invalid or empty times for overlap check
                }
            }

            for (i in timeRanges.indices) {
                for (j in i + 1 until timeRanges.size) {
                    val range1 = timeRanges[i]
                    val range2 = timeRanges[j]
                    // Check for overlap: max(start1, start2) < min(end1, end2)
                    if (maxOf(range1.first, range2.first) < minOf(range1.second, range2.second)) {
                        return "Day $day: Activities have overlapping times."
                    }
                }
            }
        }
        return null // No errors
    }


    // --- UPDATED savePackage FUNCTION ---
    fun savePackage() = viewModelScope.launch {
        if (!validate()) {
            return@launch
        }
        _uiState.update { it.copy(isSaving = true) }
        try {
            val currentState = _uiState.value
            val newImagesToUpload = convertUrisToPackageImages(currentState)
            val finalPackageData = createFinalTravelPackage(currentState)

            if (currentState.isEditing && currentState.packageId != null) {
                // --- CORRECTED EDIT LOGIC ---
                // Call the new update function with all the necessary data
                repository.updatePackageWithImages(
                    packageToUpdate = finalPackageData,
                    newImages = newImagesToUpload,
                    removedImageIds = currentState.removedImageIds
                )
            } else {
                // --- CREATE LOGIC (Unchanged) ---
                repository.createPackageWithImages(finalPackageData, newImagesToUpload)
            }

            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        } catch (e: Exception) {
            Log.e("SavePackage", "Error saving package", e)
            _uiState.update { it.copy(isSaving = false, error = e.message ?: "An unknown error occurred") }
        }
    }

    // ... (rest of the ViewModel is unchanged)
    fun onPackageNameChange(name: String) = _uiState.update { it.copy(packageName = name) }
    fun onDescriptionChange(desc: String) = _uiState.update { it.copy(packageDescription = desc) }
    fun onLocationChange(location: String) = _uiState.update { it.copy(location = location) }
    fun onDurationChange(days: String) {
        val filteredDays = days.filter { it.isDigit() }
        val newDuration = filteredDays.toIntOrNull() ?: 0
        _uiState.update { state ->
            val cleanedItineraries = state.itineraries.filter { it.day <= newDuration }
            state.copy(durationDays = filteredDays, itineraries = cleanedItineraries)
        }
    }
    fun onPriceChange(type: String, price: String) {
        val newPricing = _uiState.value.pricing.toMutableMap()
        newPricing[type] = price
        _uiState.update { it.copy(pricing = newPricing) }
    }
    fun addImage(uri: Uri) = _uiState.update { it.copy(newImageUris = it.newImageUris + uri) }
    fun removeImage(image: Any) {
        when (image) {
            is PackageImage -> _uiState.update { it.copy(removedImageIds = it.removedImageIds + image.imageId) }
            is Uri -> _uiState.update { it.copy(newImageUris = it.newImageUris - image) }
        }
    }
    fun addDepartureDate() {
        val newOption = DepartureAndEndTime()
        _uiState.update { it.copy(packageOptions = it.packageOptions + newOption) }
    }
    fun removeDepartureDate(id: String) {
        _uiState.update { st -> st.copy(packageOptions = st.packageOptions.filter { it.id != id }) }
    }
    fun onDepartureDateChange(id: String, newStartDate: com.google.firebase.Timestamp) {
        _uiState.update { state ->
            val updatedOptions = state.packageOptions.map {
                if (it.id == id) it.copy(startDate = newStartDate) else it
            }
            state.copy(packageOptions = updatedOptions)
        }
    }
    fun onCapacityChange(id: String, capacity: String) {
        val cap = capacity.filter { it.isDigit() }.toIntOrNull() ?: 0
        _uiState.update { state ->
            val updatedOptions = state.packageOptions.map {
                if (it.id == id) it.copy(capacity = cap) else it
            }
            state.copy(packageOptions = updatedOptions)
        }
    }

    fun onEditItineraryItem(item: ItineraryItem) {
        _uiState.update { it.copy(editingItineraryItem = item) }
    }

    fun onAddNewItineraryItem(day: Int, tripId: String) {
        val newItem = ItineraryItem(day = day, tripId = tripId)
        _uiState.update { it.copy(editingItineraryItem = newItem) }
    }

    fun onDismissItineraryDialog() {
        _uiState.update { it.copy(editingItineraryItem = null) }
    }

    fun upsertItineraryItem(item: ItineraryItem) {
        _uiState.update { currentState ->
            val itineraries = currentState.itineraries.toMutableList()
            val index = itineraries.indexOfFirst { it.itineraryId == item.itineraryId }
            if (index != -1) {
                itineraries[index] = item
            } else {
                itineraries.add(item)
            }
            currentState.copy(itineraries = itineraries.sortedBy { timeToMinutes(it.startTime) ?: 9999 }, editingItineraryItem = null)
        }
    }
    fun removeTripFromItinerary(itemToRemove: ItineraryItem) {
        _uiState.update {
            it.copy(itineraries = it.itineraries.filter { item -> item.itineraryId != itemToRemove.itineraryId })
        }
    }
    private suspend fun convertUrisToPackageImages(state: ManageTravelPackageUiState): List<PackageImage> {
        val existingImageCount = state.initialImages.size - state.removedImageIds.size
        return state.newImageUris.mapNotNull { uri ->
            val base64String = uriToBase64(application, uri)

            if (base64String != null) {
                PackageImage(
                    base64Data = base64String,
                    order = existingImageCount + state.newImageUris.indexOf(uri)
                )
            } else {
                Log.w("ViewModel", "Failed to convert URI to Base64 and skipped it: $uri")
                null
            }
        }
    }
    private fun createFinalTravelPackage(state: ManageTravelPackageUiState): TravelPackage {
        return TravelPackage(
            packageId = state.packageId ?: "",
            packageName = state.packageName,
            packageDescription = state.packageDescription,
            location = state.location,
            durationDays = state.durationDays.toIntOrNull() ?: 0,
            pricing = state.pricing.mapValues { (_, v) -> v.toDoubleOrNull() ?: 0.0 }.filterValues { it > 0.0 },
            itineraries = state.itineraries,
            packageOption = state.packageOptions,
            createdAt = state.createdAt
        )
    }
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

}