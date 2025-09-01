package com.example.mad_assignment.ui.packagedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.respository.TravelPackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _uiState = MutableStateFlow<PackageDetailUiState>(PackageDetailUiState.Loading)
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    private val _paxCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val paxCounts: StateFlow<Map<String, Int>> = _paxCounts.asStateFlow()

    private val _departures = MutableStateFlow<List<DepartureDate>>(emptyList())
    val departures: StateFlow<List<DepartureDate>> = _departures.asStateFlow()

    // New state for the user's selected date
    private val _selectedDepartureId = MutableStateFlow<String?>(null)
    val selectedDepartureId: StateFlow<String?> = _selectedDepartureId.asStateFlow()

    init {
        loadPackageDetails()
        loadDepartureDates()
    }

    private fun loadPackageDetails() {
        viewModelScope.launch {
            _uiState.value = PackageDetailUiState.Loading
            try {
                val travelPackage = packageRepository.getTravelPackage(packageId)
                if (travelPackage != null) {
                    _uiState.value = PackageDetailUiState.Success(travelPackage)
                    val initialPax = travelPackage.pricing.keys.associateWith { 1 }
                    _paxCounts.value = initialPax
                } else {
                    _uiState.value = PackageDetailUiState.Error("Package not found.")
                }
            } catch (e: Exception) {
                _uiState.value = PackageDetailUiState.Error(e.message ?: "An error occurred.")
            }
        }
    }

    fun updatePaxCount(category: String, change: Int) {
        val currentCount = _paxCounts.value[category] ?: 0
        val newCount = (currentCount + change).coerceAtLeast(0)
        _paxCounts.update { it.toMutableMap().apply { this[category] = newCount } }
    }

    private fun loadDepartureDates() {
        viewModelScope.launch {
            packageRepository.getDepartureDates(packageId)
                .collect { dates ->
                    _departures.value = dates
                }
        }
    }

    fun selectDeparture(departureId: String) {
        _selectedDepartureId.value = departureId
    }
}