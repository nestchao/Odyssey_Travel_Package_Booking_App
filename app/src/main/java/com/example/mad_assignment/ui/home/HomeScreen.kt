package com.example.mad_assignment.ui.home

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import com.example.mad_assignment.data.model.TravelPackage
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import java.util.Locale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.respository.NotificationRepository
import com.example.mad_assignment.ui.notifications.NotificationsScreen
import com.example.mad_assignment.ui.notifications.NotificationsViewModel
import com.example.mad_assignment.ui.notifications.NotificationsViewModelFactory
import com.google.firebase.firestore.FirebaseFirestore
import coil.request.ImageRequest
import com.example.mad_assignment.ui.home.TravelPackageWithImages
import com.example.mad_assignment.util.toDataUri

@Composable
fun HomeScreen(
    onPackageClick: (String) -> Unit,
    onBellClick: (String) -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            EnhancedBottomNavigationBar()
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    EnhancedLoadingState()
                }
                is HomeUiState.Success -> {
                    EnhancedHomeContent(
                        packages = state.packages,
                        onPackageClick = onPackageClick,
                        onBellClick = onBellClick
                    )
                }
                is HomeUiState.Error -> {
                    EnhancedErrorState(message = state.message)
                }
            }
        }
    }
}

@Composable
fun EnhancedLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Discovering amazing places...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EnhancedErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
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
                "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { /* TODO: Retry */ },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
fun EnhancedHomeContent(
    packages: List<TravelPackageWithImages>,
    onPackageClick: (String) -> Unit,
    onBellClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { EnhancedHomeHeader(onBellClick) }
        item { WelcomeSection() }
        item {
            EnhancedAroundYouSection(
                featuredPackages = packages.take(3),
                onPackageClick = onPackageClick
            )
        }
        item { PopularDestinationsSection() }
        item {
            EnhancedSectionHeader(
                title = "All Packages",
                subtitle = "${packages.size} amazing destinations",
                onViewMoreClick = { /* TODO: View More Functions */ }
            )
        }
        items(packages, key = { it.travelPackage.packageId }) { travelPackageWithImages ->
            EnhancedPackageCard(
                packageData = travelPackageWithImages,
                onClick = { onPackageClick(travelPackageWithImages.travelPackage.packageId) }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun EnhancedHomeHeader(
    onBellClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Good Morning! ☀️",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Where to next?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { /* TODO: Shopping Cart */ },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.ShoppingBag,
                                    contentDescription = "Shopping Cart",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    val notificationViewModel: NotificationsViewModel = viewModel(
                        factory = NotificationsViewModelFactory(NotificationRepository(NotificationsDataSource(FirebaseFirestore.getInstance())))
                    )
                    val unreadCount by notificationViewModel.unreadCount.collectAsState()

                    Surface(
                        onClick = { onBellClick("notifications") },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                unreadCount.toString(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enhanced Search Bar
            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = {
                    Text(
                        "Search destinations, packages...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Surface(
                        onClick = { /* TODO: Filter */ },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WelcomeSection() {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var locationText by remember { mutableStateOf("Tap to find your location") }
        val context = LocalContext.current
        val locationPermissionsState = rememberMultiplePermissionsState(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

        LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
            if (locationPermissionsState.allPermissionsGranted) {
                locationText = "Fetching location..."
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            try {
                                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (addresses?.isNotEmpty() == true) {
                                    val address = addresses[0]
                                    val city = address.locality ?: "Unknown City"
                                    val country = address.countryName ?: "Unknown Country"
                                    locationText = "$city, $country"
                                    Log.d("Location", "Current location: $locationText")
                                } else {
                                    locationText = "Location not found"
                                }
                            } catch (e: Exception) {
                                locationText = "Could not get address"
                                Log.e("Geocoder", "Failed to get address", e)
                            }
                        } else {
                            locationText = "Location is disabled"
                        }
                    }.addOnFailureListener { e ->
                        locationText = "Failed to get location"
                        Log.e("Location", "Failed to get last location", e)
                    }
                } catch (e: SecurityException) {
                    locationText = "Permission denied"
                    Log.e("Location", "Location permission not granted", e)
                }
            } else {
                locationText = "Permission needed"
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable {
                    if (!locationPermissionsState.allPermissionsGranted) {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(12.dp)
            ) {}
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "You are currently at",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PopularDestinationsSection() {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trending Destinations 🔥",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val destinations = listOf(
                "🏝️ Langkawi" to Color(0xFF4FC3F7),
                "🏔️ Cameron" to Color(0xFF66BB6A),
                "🌊 Penang" to Color(0xFFFFB74D),
                "🏛️ Malacca" to Color(0xFFE57373)
            )

            items(destinations.size) { index ->
                val (destination, color) = destinations[index]
                Surface(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(16.dp),
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.width(120.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedSectionHeader(
    title: String,
    subtitle: String = "",
    onViewMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TextButton(
            onClick = onViewMoreClick,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "View All",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun EnhancedAroundYouSection(
    featuredPackages: List<TravelPackageWithImages>,
    onPackageClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Near You 📍",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Discover local gems",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = { /* TODO */ },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "See All",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(featuredPackages) { packageItem ->
                EnhancedAroundYouCard(
                    title = packageItem.travelPackage.packageName, // <-- CHANGED
                    imageUrl = packageItem.images.firstOrNull()?.base64Data ?: "", // <-- FIXED
                    price = packageItem.travelPackage.pricing.values.minOrNull() ?: 0.0, // <-- CHANGED
                    onClick = { onPackageClick(packageItem.travelPackage.packageId) } // <-- CHANGED
                )
            }
        }
    }
}

@Composable
fun EnhancedAroundYouCard(
    title: String,
    imageUrl: String,
    price: Double,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.size(200.dp, 280.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            val decodedImage = toDataUri(imageUrl)
            if (decodedImage == null) {
                Log.e("EnhancedAroundYouCard", "Failed to decode image for '$title'. imageUrl was: ${if (imageUrl.isBlank()) "BLANK" else imageUrl.take(50) + "..."}")
            } else {
                Log.d("EnhancedAroundYouCard", "Successfully decoded image for '$title'. Byte array size: ${decodedImage.size} bytes.")
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data( toDataUri(imageUrl)).build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 0f,
                            endY = 800f
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "From RM ${"%.0f".format(price)}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedPackageCard(
    packageData: TravelPackageWithImages,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.height(120.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(toDataUri(packageData.images.firstOrNull()?.base64Data))
                    .build(),
                contentDescription = packageData.travelPackage.packageName,
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = packageData.travelPackage.packageName,
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
                            "${packageData.travelPackage.durationDays}D ${packageData.travelPackage.durationDays - 1}N",
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
                        val price = packageData.travelPackage.pricing.values.minOrNull() ?: 0.0
                        Text(
                            "From RM ${"%.0f".format(price)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "RM ${"%.0f".format(price)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedBottomNavigationBar() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        Triple("Home", Icons.Outlined.Home, Icons.Filled.Home),
        Triple("Explore", Icons.Outlined.Explore, Icons.Filled.Explore),
        Triple("Bookings", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
        Triple("Profile", Icons.Outlined.Person, Icons.Filled.Person)
    )

    NavigationBar(
        modifier = Modifier.shadow(elevation = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, (label, outlinedIcon, filledIcon) ->
            val isSelected = selectedItem == index
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(300), label = ""
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { selectedItem = index },
                icon = {
                    Icon(
                        if (isSelected) filledIcon else outlinedIcon,
                        contentDescription = label,
                        tint = animatedColor
                    )
                },
                label = {
                    Text(
                        label,
                        color = animatedColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}