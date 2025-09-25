// src/main/java/com/example/mad_assignment/ui/wishlist/WishlistScreen.kt
package com.example.mad_assignment.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete // Import the Delete icon
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// NO CHANGE to TravelPackage model import (it's not the direct list item type here)
import com.example.mad_assignment.data.model.TravelPackageWithImages // Import this
import coil.compose.AsyncImage // Import AsyncImage
import coil.request.ImageRequest // Import ImageRequest
import com.example.mad_assignment.util.toDataUri // Assuming this utility function exists
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.compose.ui.layout.ContentScale // Import ContentScale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onNavigateBack: () -> Unit,
    onPackageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    // Refresh when the screen is composed or recomposed
    LaunchedEffect(Unit) {
        viewModel.loadWishlist()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Wishlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.loadWishlist() // Refresh before going back
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Refresh button - always show
                    IconButton(
                        onClick = { viewModel.loadWishlist() }
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Clear button - only show when there are items
                    if (uiState is WishlistUiState.Success &&
                        (uiState as WishlistUiState.Success).wishlistPackages.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is WishlistUiState.Loading -> {
                LoadingContent(paddingValues)
            }

            is WishlistUiState.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "You have ${state.wishlistPackages.size} item${if (state.wishlistPackages.size == 1) "" else "s"} in your wishlist",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        // IMPORTANT: 'travelPackage' here is actually TravelPackageWithImages
                        items(
                            items = state.wishlistPackages,
                            key = { it.travelPackage.packageId } // Key is from the nested TravelPackage
                        ) { travelPackageWithImages -> // Changed parameter name to reflect type
                            WishlistPackageCard(
                                travelPackageWithImages = travelPackageWithImages, // Pass the TravelPackageWithImages object
                                onRemove = {
                                    viewModel.removeFromWishlistByPackageId(travelPackageWithImages.travelPackage.packageId)
                                },
                                onClick = {
                                    viewModel.loadWishlist() // Refresh before navigating
                                    onPackageClick(travelPackageWithImages.travelPackage.packageId) // Access packageId from nested TravelPackage
                                }
                            )
                        }
                    }
                }
            }

            is WishlistUiState.Empty -> {
                EmptyContent(paddingValues)
            }

            is WishlistUiState.Error -> {
                ErrorContent(
                    paddingValues = paddingValues,
                    message = state.message,
                    onRetry = { viewModel.loadWishlist() }
                )
            }
        }
    }

    // Clear confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text("Clear Wishlist")
            },
            text = {
                Text("Are you sure you want to clear all items from your wishlist? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        viewModel.clearWishlist()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WishlistPackageCard(
    travelPackageWithImages: TravelPackageWithImages, // IMPORTANT: Changed parameter type
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    // Extract TravelPackage and primary image URL from TravelPackageWithImages
    val travelPackage = travelPackageWithImages.travelPackage
    val primaryImageUrl = travelPackageWithImages.images.firstOrNull()?.base64Data
    val context = LocalContext.current // Get current context for ImageRequest

    Card(
        modifier = Modifier
            .fillMaxWidth(), // Removed .clickable here as it's now wrapped in the row
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() } // Make the whole card row clickable
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically // Align items vertically
        ) {
            // Package image or placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                // Use AsyncImage if primaryImageUrl is available
                if (!primaryImageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(toDataUri(primaryImageUrl)) // Use toDataUri for Base64 images
                            .crossfade(true)
                            .build(),
                        contentDescription = travelPackage.packageName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Otherwise, show a placeholder icon
                    Icon(
                        Icons.Outlined.Favorite, // Placeholder icon for wishlist
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f) // Takes remaining space
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = travelPackage.packageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = travelPackage.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${travelPackage.durationDays}D${travelPackage.durationDays - 1}N",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val minPrice = travelPackage.pricing.values.minOrNull() ?: 0.0
                        if (minPrice > 0) {
                            Text(
                                text = "From RM ${String.format("%.0f", minPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.CenterVertically) // Align to center
            ) {
                Icon(
                    Icons.Default.Delete, // Changed to a more explicit delete icon
                    contentDescription = "Remove from Wishlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun LoadingContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your wishlist...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Wishlist is Empty",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start adding travel packages to your wishlist and they will appear here for easy access.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
            )
        }
    }
}

@Composable
fun ErrorContent(
    paddingValues: PaddingValues,
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}