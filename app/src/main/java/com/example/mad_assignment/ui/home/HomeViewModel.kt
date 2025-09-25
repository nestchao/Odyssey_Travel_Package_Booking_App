package com.example.mad_assignment.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.repository.TravelPackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val travelPackageRepository: TravelPackageRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        travelPackageRepository.getTravelPackagesWithImages()
            .onStart {
                Log.d("HomeViewModel", "Starting to fetch travel packages")
            }
            .map<List<TravelPackageWithImages>, HomeUiState> { packagesWithImages ->
                Log.d("HomeViewModel", "Successfully loaded ${packagesWithImages.size} packages")
                HomeUiState.Success(
                    packages = packagesWithImages
                    )
            }
            .catch { exception ->
                Log.e("HomeViewModel", "Error loading packages", exception)
                emit(HomeUiState.Error(exception.message ?: "An unknown error occurred"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading
            )
}