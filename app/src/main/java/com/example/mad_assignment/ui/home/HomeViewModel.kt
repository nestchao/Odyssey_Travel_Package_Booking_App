package com.example.mad_assignment.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.TravelPackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val travelPackageRepository: TravelPackageRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPackages()
    }

    private fun loadPackages() {
        viewModelScope.launch {
            travelPackageRepository.getTravelPackages()
                .catch { exception ->
                    _uiState.value = HomeUiState.Error(exception.message ?: "An unknown error occurred")
                }
                .collect { allPackages ->
                    // The state now just holds the single list
                    _uiState.value = HomeUiState.Success(allPackages)
                }
        }
    }
}