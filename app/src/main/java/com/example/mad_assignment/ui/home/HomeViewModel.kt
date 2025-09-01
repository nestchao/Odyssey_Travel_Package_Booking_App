package com.example.mad_assignment.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.respository.TravelPackageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val travelPackageRepository: TravelPackageRepository
): ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPackages()
    }

    private fun loadPackages() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading

            val featuredFlow = travelPackageRepository.getFeaturedPackages()
            val allPackagesFlow = travelPackageRepository.getTravelPackages()

            featuredFlow.combine(allPackagesFlow) { featuredList, allList ->
                HomeUiState.Success(
                    featuredPackages = featuredList,
                    allPackages = allList
                )
            }.catch { exception ->
                _uiState.value = HomeUiState.Error(exception.message ?: "An unknown error occurred")
            }.collect { combinedSuccessState ->
                _uiState.value = combinedSuccessState
            }
        }
    }
}
