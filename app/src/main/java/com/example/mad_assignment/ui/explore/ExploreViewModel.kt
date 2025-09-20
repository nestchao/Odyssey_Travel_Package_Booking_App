package com.example.mad_assignment.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.ui.home.TravelPackageWithImages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOption(val displayName: String) {
    POPULARITY("Popularity"),
    PRICE_ASC("Price: Low to High"),
    PRICE_DESC("Price: High to Low"),
    NEWEST("Newest")
}

data class ExploreState(
    val searchQuery: String = "",
    val selectedCategories: Set<String> = emptySet(),
    val selectedSortOption: SortOption = SortOption.POPULARITY
)

sealed interface ExploreUiState {
    object Loading : ExploreUiState
    data class Success(
        val allPackages: List<TravelPackageWithImages>,
        val displayedPackages: List<TravelPackageWithImages>,
        val filterState: ExploreState
    ) : ExploreUiState
    data class Error(val message: String) : ExploreUiState
}


@HiltViewModel
class ExploreViewModel @Inject constructor(
    repository: TravelPackageRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(ExploreState())

    val categories = listOf("Beach", "Mountain", "City Break", "Adventure", "Cultural", "Nature")

    val uiState: StateFlow<ExploreUiState> = combine(
        repository.getTravelPackagesWithImages(),
        _filterState
    ) { allPackages, filter ->

        val searchedList = if (filter.searchQuery.isBlank()) {
            allPackages
        } else {
            allPackages.filter {
                it.travelPackage.packageName.contains(filter.searchQuery, ignoreCase = true) ||
                        it.travelPackage.location.contains(filter.searchQuery, ignoreCase = true) ||
                        it.travelPackage.packageDescription.contains(filter.searchQuery, ignoreCase = true)
            }
        }

        val categorizedList = if (filter.selectedCategories.isEmpty()) {
            searchedList
        } else {
            searchedList.filter { pkg ->
                filter.selectedCategories.any { category ->
                    pkg.travelPackage.packageName.contains(category, ignoreCase = true) ||
                            pkg.travelPackage.location.contains(category, ignoreCase = true)
                }
            }
        }

        val sortedList = when (filter.selectedSortOption) {
            SortOption.POPULARITY -> categorizedList.sortedByDescending { it.travelPackage.pricing.size }
            SortOption.PRICE_ASC -> categorizedList.sortedBy { it.travelPackage.pricing.values.minOrNull() ?: Double.MAX_VALUE }
            SortOption.PRICE_DESC -> categorizedList.sortedByDescending { it.travelPackage.pricing.values.minOrNull() ?: 0.0 }
            SortOption.NEWEST -> categorizedList.sortedByDescending { it.travelPackage.createdAt }
        }

        ExploreUiState.Success(
            allPackages = allPackages,
            displayedPackages = sortedList,
            filterState = filter
        ) as ExploreUiState
    }.catch { e ->
        emit(ExploreUiState.Error(e.message ?: "An unknown error occurred"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ExploreUiState.Loading
    )

    fun onSearchQueryChanged(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: String) {
        _filterState.update { currentState ->
            val newCategories = currentState.selectedCategories.toMutableSet()
            if (newCategories.contains(category)) {
                newCategories.remove(category)
            } else {
                newCategories.add(category)
            }
            currentState.copy(selectedCategories = newCategories)
        }
    }

    fun onSortChanged(sortOption: SortOption) {
        _filterState.update { it.copy(selectedSortOption = sortOption) }
    }

    fun clearFilters() {
        _filterState.update { it.copy(selectedCategories = emptySet()) }
    }
}