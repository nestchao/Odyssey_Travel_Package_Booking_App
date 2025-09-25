package com.example.mad_assignment.ui.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete // NEW - for wishlist remove button
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.ui.home.EnhancedPackageCard
import com.example.mad_assignment.ui.home.HomeUiState
import com.example.mad_assignment.data.model.TravelPackageWithImages // NEW - needed for EnhancedPackageCard
import coil.compose.AsyncImage // NEW
import coil.request.ImageRequest // NEW
import com.example.mad_assignment.util.toDataUri // NEW
import androidx.compose.ui.platform.LocalContext // NEW
import androidx.compose.ui.layout.ContentScale // NEW
import androidx.compose.ui.text.style.TextOverflow


@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onPackageClick: (String) -> Unit
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val latestPackages by viewModel.latestPackages.collectAsStateWithLifecycle()
    val wishlistPackages by viewModel.wishlistPackages.collectAsStateWithLifecycle() // NEW
    val recentlyViewedPackages by viewModel.recentlyViewedPackages.collectAsStateWithLifecycle() // NEW

    val contentVisible by remember {
        derivedStateOf { searchQuery.isNotBlank() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            EnhancedSearchTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onNavigateBack = onNavigateBack,
                onClearSearch = { viewModel.onSearchQueryChange("") }
            )

            AnimatedContent(
                targetState = contentVisible,
                transitionSpec = {
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(400)) togetherWith
                            slideOutVertically(
                                targetOffsetY = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.fillMaxSize(),
                label = "content_transition"
            ) { showResults ->
                if (showResults) {
                    SearchResults(
                        searchResults = searchResults,
                        searchQuery = searchQuery,
                        onPackageClick = onPackageClick
                    )
                } else {
                    // MODIFIED: Pass the new states and click handlers down
                    SearchEmptyState(
                        onSuggestionClick = viewModel::onSearchQueryChange,
                        latestPackagesState = latestPackages,
                        wishlistPackagesState = wishlistPackages, // NEW
                        recentlyViewedPackagesState = recentlyViewedPackages, // NEW
                        onPackageClick = onPackageClick,
                        onRemoveFromWishlist = viewModel::removeFromWishlistByPackageId // NEW
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onClearSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchBarScale by animateFloatAsState(
        targetValue = if (searchQuery.isNotEmpty()) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "search_bar_scale"
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = if (searchQuery.isNotEmpty()) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .scale(searchBarScale)
                    .focusRequester(focusRequester)
                    .clip(RoundedCornerShape(24.dp)),
                placeholder = {
                    Text(
                        "Where do you want to go?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchQuery.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        IconButton(onClick = onClearSearch) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SearchResults(
    searchResults: HomeUiState,
    searchQuery: String,
    onPackageClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = slideInVertically() + fadeIn()
            ) {
                ResultsHeader(searchQuery = searchQuery, resultsCount =
                    when (searchResults) {
                        is HomeUiState.Success -> searchResults.packages.size
                        else -> 0
                    }
                )
            }
        }

        when (searchResults) {
            is HomeUiState.Loading -> {
                item {
                    LoadingState()
                }
            }
            is HomeUiState.Success -> {
                if (searchResults.packages.isEmpty()) {
                    item {
                        EmptyResults(searchQuery = searchQuery)
                    }
                } else {
                    items(
                        searchResults.packages,
                        key = { it.travelPackage.packageId }
                    ) { packageWithImages ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { it / 2 }
                            ) + fadeIn()
                        ) {
                            EnhancedPackageCard(
                                packageData = packageWithImages,
                                onClick = { onPackageClick(packageWithImages.travelPackage.packageId) }
                            )
                        }
                    }
                }
            }
            is HomeUiState.Error -> {
                item {
                    ErrorState(message = searchResults.message)
                }
            }
        }
    }
}

@Composable
private fun SearchEmptyState(
    onSuggestionClick: (String) -> Unit,
    latestPackagesState: HomeUiState,
    wishlistPackagesState: HomeUiState, // NEW
    recentlyViewedPackagesState: HomeUiState, // NEW
    onPackageClick: (String) -> Unit,
    onRemoveFromWishlist: (String) -> Unit // NEW
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    // Renamed "Nearby" to "Recently Viewed"
    val tabs = listOf("Wishlist", "Recently Viewed", "New")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically() + fadeIn(tween(durationMillis = 100))
            ) {
                PopularSearches(onSuggestionClick = onSuggestionClick)
            }
        }

        item {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically() + fadeIn(tween(durationMillis = 200))
            ) {
                EnhancedFilterTabs(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it }
                )
            }
        }

        item {
            AnimatedContent(
                targetState = selectedTabIndex,
                label = "tab_content_transition",
                transitionSpec = {
                    fadeIn(animationSpec = tween(200, 100)) togetherWith
                            fadeOut(animationSpec = tween(100))
                }
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> WishlistTabContent( // NEW
                        wishlistPackagesState = wishlistPackagesState,
                        onPackageClick = onPackageClick,
                        onRemoveFromWishlist = onRemoveFromWishlist
                    )
                    1 -> RecentlyViewedTabContent( // NEW
                        recentlyViewedPackagesState = recentlyViewedPackagesState,
                        onPackageClick = onPackageClick
                    )
                    2 -> LatestPackagesContent(
                        latestPackagesState = latestPackagesState,
                        onPackageClick = onPackageClick
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedFilterTabs(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            containerColor = Color.Transparent,
            indicator = { },
            divider = { }
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "tab_scale"
                )

                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier
                        .scale(animatedScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent
                        ),
                    text = {
                        Text(
                            title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ResultsHeader(searchQuery: String, resultsCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Search Results",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$resultsCount result${if (resultsCount != 1) "s" else ""} for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PopularSearches(onSuggestionClick: (String) -> Unit) {
    val suggestions = listOf(
        "Penang", "Langkawi", "Cameron Highlands",
        "Genting", "Malacca", "Kuala Lumpur", "Island Hopping"
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Popular Destinations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "chip_scale"
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = slideInHorizontally(
                        initialOffsetX = { it / 2 }
                    ) + fadeIn(
                        tween(durationMillis = 100 + index * 50)
                    )
                ) {
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            Text(
                                suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .scale(scale),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }
        }
    }
}

// Removed the duplicate EnhancedFilterTabs and SearchPrompt functions

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                "Searching for packages...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyResults(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(0.4f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No results found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "We couldn't find any packages for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                "Try searching with different keywords",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Oops! Something went wrong",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LatestPackagesContent(
    latestPackagesState: HomeUiState,
    onPackageClick: (String) -> Unit
) {
    when (latestPackagesState) {
        is HomeUiState.Loading -> {
            LoadingState()
        }
        is HomeUiState.Success -> {
            if (latestPackagesState.packages.isEmpty()) {
                TabContentPlaceholder(
                    title = "No New Packages",
                    message = "Check back later for freshly added adventures!"
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    latestPackagesState.packages.forEach { packageWithImages ->
                        EnhancedPackageCard(
                            packageData = packageWithImages,
                            onClick = { onPackageClick(packageWithImages.travelPackage.packageId) }
                        )
                    }
                }
            }
        }
        is HomeUiState.Error -> {
            ErrorState(message = latestPackagesState.message)
        }
    }
}

// NEW: Wishlist Content Composable
@Composable
private fun WishlistTabContent(
    wishlistPackagesState: HomeUiState,
    onPackageClick: (String) -> Unit,
    onRemoveFromWishlist: (String) -> Unit
) {
    when (wishlistPackagesState) {
        is HomeUiState.Loading -> {
            LoadingState()
        }
        is HomeUiState.Success -> {
            if (wishlistPackagesState.packages.isEmpty()) {
                TabContentPlaceholder(
                    title = "Your Wishlist is Empty",
                    message = "Add packages to your wishlist to see them here."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    wishlistPackagesState.packages.forEach { packageWithImages ->
                        // Using a slightly modified card for wishlist to include remove button
                        WishlistPackageSearchCard(
                            travelPackageWithImages = packageWithImages,
                            onRemove = { onRemoveFromWishlist(packageWithImages.travelPackage.packageId) },
                            onClick = { onPackageClick(packageWithImages.travelPackage.packageId) }
                        )
                    }
                }
            }
        }
        is HomeUiState.Error -> {
            ErrorState(message = wishlistPackagesState.message)
        }
    }
}

// NEW: Recently Viewed Content Composable
@Composable
private fun RecentlyViewedTabContent(
    recentlyViewedPackagesState: HomeUiState,
    onPackageClick: (String) -> Unit
) {
    when (recentlyViewedPackagesState) {
        is HomeUiState.Loading -> {
            LoadingState()
        }
        is HomeUiState.Success -> {
            if (recentlyViewedPackagesState.packages.isEmpty()) {
                TabContentPlaceholder(
                    title = "No Recently Viewed Packages",
                    message = "Explore packages to see them in your recent history."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    recentlyViewedPackagesState.packages.forEach { packageWithImages ->
                        EnhancedPackageCard(
                            packageData = packageWithImages,
                            onClick = { onPackageClick(packageWithImages.travelPackage.packageId) }
                        )
                    }
                }
            }
        }
        is HomeUiState.Error -> {
            ErrorState(message = recentlyViewedPackagesState.message)
        }
    }
}


@Composable
private fun TabContentPlaceholder(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


// NEW: Custom Card for Wishlist in Search, similar to EnhancedPackageCard but with a remove option
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistPackageSearchCard(
    travelPackageWithImages: TravelPackageWithImages,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val travelPackage = travelPackageWithImages.travelPackage
    val primaryImageUrl = travelPackageWithImages.images.firstOrNull()?.base64Data
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!primaryImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(toDataUri(primaryImageUrl))
                            .crossfade(true)
                            .build(),
                        contentDescription = travelPackage.packageName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = travelPackage.packageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${travelPackage.durationDays}D ${travelPackage.durationDays - 1}N",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        val price = travelPackage.pricing.values.minOrNull() ?: 0.0
                        Text(
                            "From RM ${"%.0f".format(price)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove from Wishlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}