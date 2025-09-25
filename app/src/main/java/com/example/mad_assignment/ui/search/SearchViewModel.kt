package com.example.mad_assignment.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.ui.home.HomeUiState
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val travelPackageRepository: TravelPackageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    val searchResults: StateFlow<HomeUiState> =
        travelPackageRepository.getTravelPackagesWithImages()
            .combine(searchQuery) { packages, query ->
                if (query.isBlank()) {
                    emptyList<TravelPackageWithImages>()
                } else {
                    packages.filter {
                        it.travelPackage.packageName.contains(query, ignoreCase = true) ||
                                it.travelPackage.location.contains(query, ignoreCase = true)
                    }
                }
            }
            .map<List<TravelPackageWithImages>, HomeUiState> { filteredPackages ->
                HomeUiState.Success(filteredPackages)
            }
            .catch { exception ->
                emit(HomeUiState.Error(exception.message ?: "An error occurred"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading
            )

    val latestPackages: StateFlow<HomeUiState> =
        travelPackageRepository.getTravelPackagesWithImages()
            .map<List<TravelPackageWithImages>, HomeUiState> { allPackages ->
                val sortedPackages = allPackages
                    .sortedByDescending { it.travelPackage.createdAt ?: Timestamp(0, 0) }
                    .take(5)
                HomeUiState.Success(sortedPackages)
            }
            .catch { exception ->
                emit(HomeUiState.Error(exception.message ?: "Could not load latest packages"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading
            )
}