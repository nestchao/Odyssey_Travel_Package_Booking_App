package com.example.mad_assignment.ui.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.ui.home.EnhancedErrorState
import com.example.mad_assignment.ui.home.EnhancedLoadingState
import com.example.mad_assignment.ui.home.TabletPackageGridCard
import com.example.mad_assignment.ui.home.TravelPackageWithImages

@Composable
fun ExploreScreen(
    onPackageClick: (String) -> Unit
) {
    val viewModel: ExploreViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (val state = uiState) {
                is ExploreUiState.Loading -> EnhancedLoadingState()
                is ExploreUiState.Error -> EnhancedErrorState(message = state.message)
                is ExploreUiState.Success -> ExploreContent(
                    state = state,
                    viewModel = viewModel,
                    onPackageClick = onPackageClick
                )
            }
        }
    }
}

@Composable
fun ExploreContent(
    state: ExploreUiState.Success,
    viewModel: ExploreViewModel,
    onPackageClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            ExploreHeader()
        }
        item {
            SearchBar(
                query = state.filterState.searchQuery,
                onQueryChanged = viewModel::onSearchQueryChanged
            )
        }
        item {
            CategoryFilters(
                categories = viewModel.categories,
                selectedCategories = state.filterState.selectedCategories,
                onCategorySelected = viewModel::onCategorySelected
            )
        }
        item {
            ResultsHeader(
                resultsCount = state.displayedPackages.size,
                selectedSortOption = state.filterState.selectedSortOption,
                onSortChanged = viewModel::onSortChanged,
                onClearFilters = viewModel::clearFilters,
                isFilterActive = state.filterState.selectedCategories.isNotEmpty()
            )
        }

        if (state.displayedPackages.isEmpty()) {
            item {
                NoResultsView()
            }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier
                        .heightIn(max = 2000.dp) // Adjust height as needed
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false // LazyColumn handles scrolling
                ) {
                    items(state.displayedPackages, key = { it.travelPackage.packageId }) { packageData ->
                        // Reusing the card from HomeScreen for consistency
                        TabletPackageGridCard(
                            packageData = packageData,
                            onClick = { onPackageClick(packageData.travelPackage.packageId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreHeader() {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Find your next adventure",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search for places, packages...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        singleLine = true,
        maxLines = 1,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilters(
    categories: List<String>,
    selectedCategories: Set<String>,
    onCategorySelected: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            "Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategories.contains(category)
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
fun ResultsHeader(
    resultsCount: Int,
    selectedSortOption: SortOption,
    onSortChanged: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    isFilterActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Results ($resultsCount)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isFilterActive) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear")
                }
            }
            SortDropdown(
                selectedOption = selectedSortOption,
                onOptionSelected = onSortChanged
            )
        }
    }
}

@Composable
fun SortDropdown(
    selectedOption: SortOption,
    onOptionSelected: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Sort, contentDescription = "Sort by", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(selectedOption.displayName, fontWeight = FontWeight.Medium)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NoResultsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Packages Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Try adjusting your search or filters to find what you're looking for.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}