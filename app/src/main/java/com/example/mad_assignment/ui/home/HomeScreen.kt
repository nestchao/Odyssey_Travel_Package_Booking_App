package com.example.mad_assignment.ui.home

import android.Manifest
import android.location.Geocoder
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.datasource.ScheduledNotificationDataSource
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.repository.NotificationRepository
import com.example.mad_assignment.ui.notifications.NotificationsViewModel
import com.example.mad_assignment.ui.notifications.NotificationsViewModelFactory
import com.example.mad_assignment.util.toDataUri
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun HomeScreen(
    onPackageClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManagement: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        floatingActionButton = {
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isTablet) {
                TabletHomeScreen(
                    uiState = uiState,
                    onPackageClick = onPackageClick,
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToCart = onNavigateToCart,
                    onBellClick = onBellClick,
                    onNavigateToProfile = onNavigateToProfile
                )
            } else {
                PhoneHomeScreen(
                    uiState = uiState,
                    onPackageClick = onPackageClick,
                    onNavigateToSearch = onNavigateToSearch,
                    onBellClick = onBellClick,
                    onNavigateToCart = onNavigateToCart
                )
            }
        }
    }
}


@Composable
fun PhoneHomeScreen(
    uiState: HomeUiState,
    onPackageClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> EnhancedLoadingState()
            is HomeUiState.Success -> {
                EnhancedHomeContent(
                    packages = state.packages,
                    onPackageClick = onPackageClick,
                    onNavigateToSearch = onNavigateToSearch,
                    onBellClick = onBellClick,
                    onNavigateToCart = onNavigateToCart
                )
            }
            is HomeUiState.Error -> EnhancedErrorState(message = state.message)
        }
    }
}

@Composable
fun EnhancedHomeContent(
    packages: List<TravelPackageWithImages>,
    onPackageClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            EnhancedHomeHeader(
                onNavigateToSearch = onNavigateToSearch,
                onBellClick = onBellClick,
                onNavigateToCart = onNavigateToCart
            )
        }
        item { WelcomeSection() }
        item {
            EnhancedAroundYouSection(
                featuredPackages = packages.take(3),
                onPackageClick = onPackageClick
            )
        }
        item {
            EnhancedSectionHeader(
                title = "All Packages",
                subtitle = "${packages.size} amazing destinations"
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
    onNavigateToSearch: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToCart: () -> Unit
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
                        onClick = { onNavigateToCart() },
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
                        factory = NotificationsViewModelFactory(
                            NotificationRepository(
                                NotificationsDataSource(FirebaseFirestore.getInstance()),
                                ScheduledNotificationDataSource(FirebaseFirestore.getInstance()),
                                LocalContext.current
                            ),
                            LocalContext.current
                        )
                    )
                    val unreadCount by notificationViewModel.unreadCount.collectAsState()

                    Surface(
                        onClick = { onBellClick() },
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

            Surface(
                onClick = onNavigateToSearch,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search destinations, packages...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ... (Other composables like EnhancedAroundYouSection, EnhancedPackageCard remain the same) ...
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
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(featuredPackages) { packageItem ->
                EnhancedAroundYouCard(
                    title = packageItem.travelPackage.packageName,
                    imageUrl = packageItem.images.firstOrNull()?.base64Data ?: "",
                    price = packageItem.travelPackage.pricing.values.minOrNull() ?: 0.0,
                    onClick = { onPackageClick(packageItem.travelPackage.packageId) }
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(toDataUri(imageUrl)).build(),
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
fun TabletHomeScreen(
    uiState: HomeUiState,
    onPackageClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToCart: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is HomeUiState.Loading -> EnhancedLoadingState()
            is HomeUiState.Success -> {
                TabletHomeContent(
                    packages = state.packages,
                    onPackageClick = onPackageClick,
                    onNavigateToSearch = onNavigateToSearch,
                    isLandscape = isLandscape,
                    onNavigateToCart = onNavigateToCart,
                    onBellClick = onBellClick,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
            is HomeUiState.Error -> EnhancedErrorState(message = state.message)
        }
    }
}

@Composable
fun TabletHomeContent(
    packages: List<TravelPackageWithImages>,
    onPackageClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    isLandscape: Boolean,
    onNavigateToCart: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val listState = rememberLazyListState()
    val horizontalPadding = if (isLandscape) 24.dp else 16.dp

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            TabletHomeHeader(
                onNavigateToSearch = onNavigateToSearch,
                horizontalPadding = horizontalPadding,
                isLandscape = isLandscape,
                onNavigateToCart = onNavigateToCart,
                onBellClick = onBellClick,
                onNavigateToProfile = onNavigateToProfile
            )
        }
        if (!isLandscape) {
            item { WelcomeSection() }
        }
        item {
            TabletFeaturedSection(
                featuredPackages = packages.take(if (isLandscape) 4 else 3),
                onPackageClick = onPackageClick,
                horizontalPadding = horizontalPadding,
                isLandscape = isLandscape
            )
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "All Travel Packages",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${packages.size} amazing destinations to explore",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    TabletPackagesGrid(
                        packages = packages,
                        onPackageClick = onPackageClick,
                        horizontalPadding = 24.dp,
                        isLandscape = isLandscape
                    )
                }
            }
        }
    }
}

@Composable
fun TabletHomeHeader(
    onNavigateToSearch: () -> Unit,
    horizontalPadding: Dp,
    isLandscape: Boolean,
    onNavigateToCart: () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Good Morning! ☀️",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Where to next?",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WelcomeSection()
                }
                Spacer(modifier = Modifier.width(32.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        // FIXED: Added onClick handlers
                        TabletActionButton(
                            icon = Icons.Outlined.ShoppingBag,
                            badge = true,
                            onClick = onNavigateToCart
                        )
                        TabletActionButton(
                            icon = Icons.Outlined.Notifications,
                            badge = true,
                            badgeText = "3", // This can be dynamic later
                            onClick = onBellClick
                        )
                        TabletActionButton(
                            icon = Icons.Outlined.Person,
                            onClick = onNavigateToProfile
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        onClick = onNavigateToSearch,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search destinations, packages...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        } else { // Portrait Tablet
            Column(modifier = Modifier.padding(32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Good Morning! ☀️",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Where to next?",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // FIXED: Added onClick handlers
                        TabletActionButton(
                            icon = Icons.Outlined.ShoppingBag,
                            badge = true,
                            onClick = onNavigateToCart
                        )
                        TabletActionButton(
                            icon = Icons.Outlined.Notifications,
                            badge = true,
                            badgeText = "3",
                            onClick = onBellClick
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Surface(
                    onClick = onNavigateToSearch,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search destinations, packages...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}


// ... (The rest of the file remains the same) ...
@Composable
fun TabletActionButton(
    icon: ImageVector,
    badge: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (badge) {
                BadgedBox(
                    badge = {
                        if (badgeText != null) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(badgeText, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TabletFeaturedSection(
    featuredPackages: List<TravelPackageWithImages>,
    onPackageClick: (String) -> Unit,
    horizontalPadding: Dp,
    isLandscape: Boolean
) {
    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Featured Destinations ⭐",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Handpicked for you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(featuredPackages) { packageItem ->
                TabletFeaturedCard(
                    title = packageItem.travelPackage.packageName,
                    imageUrl = packageItem.images.firstOrNull()?.base64Data ?: "",
                    price = packageItem.travelPackage.pricing.values.minOrNull() ?: 0.0,
                    duration = "${packageItem.travelPackage.durationDays}D ${packageItem.travelPackage.durationDays - 1}N",
                    onClick = { onPackageClick(packageItem.travelPackage.packageId) },
                    isLarge = isLandscape
                )
            }
        }
    }
}

@Composable
fun TabletFeaturedCard(
    title: String,
    imageUrl: String,
    price: Double,
    duration: String,
    onClick: () -> Unit,
    isLarge: Boolean = false
) {
    val cardWidth = if (isLarge) 280.dp else 240.dp
    val cardHeight = if (isLarge) 360.dp else 320.dp

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.size(cardWidth, cardHeight),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(toDataUri(imageUrl))
                    .build(),
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
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 0f,
                            endY = 1000f
                        )
                    )
            )

            // Duration Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.9f)
            ) {
                Text(
                    duration,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "From RM ${"%.0f".format(price)}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TabletPackagesGrid(
    packages: List<TravelPackageWithImages>,
    onPackageClick: (String) -> Unit,
    horizontalPadding: Dp,
    isLandscape: Boolean
) {
    val columns = if (isLandscape) 3 else 2
    val rows = (packages.size + columns - 1) / columns
    val gridHeight = (rows * 200).dp // Approximate height (card height + vertical spacing)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(horizontal = horizontalPadding)
            .height(gridHeight),
        userScrollEnabled = false // Disable scrolling since it's inside LazyColumn
    ) {
        items(packages) { packageData ->
            TabletPackageGridCard(
                packageData = packageData,
                onClick = { onPackageClick(packageData.travelPackage.packageId) }
            )
        }
    }
}

@Composable
fun TabletPackageGridCard(
    packageData: TravelPackageWithImages,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(toDataUri(packageData.images.firstOrNull()?.base64Data))
                    .build(),
                contentDescription = packageData.travelPackage.packageName,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = packageData.travelPackage.packageName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    val price = packageData.travelPackage.pricing.values.minOrNull() ?: 0.0
                    Text(
                        "RM ${"%.0f".format(price)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}


// --- Shared UI Components ---

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
                .clickable {
                    if (!locationPermissionsState.allPermissionsGranted) {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = "Location Pin",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
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
fun EnhancedSectionHeader(
    title: String,
    subtitle: String = ""
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
    }
}


// --- THIS IS THE CORRECTED BOTTOM NAVIGATION BAR ---
// It is now ENTIRELY REPLACED with a navigation-aware version.

// Helper data class for bottom navigation items
data class BottomNavItem(
    val label: String,
    val route: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
)

@Composable
fun EnhancedBottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("Home", "home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem("Explore", "explore", Icons.Outlined.Explore, Icons.Filled.Explore),
        BottomNavItem("Bookings", "bookings", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
        BottomNavItem("Profile", "profile", Icons.Outlined.Person, Icons.Filled.Person)
    )

    NavigationBar(
        modifier = Modifier.shadow(elevation = 8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(300), label = ""
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        if (isSelected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label,
                        tint = animatedColor
                    )
                },
                label = {
                    Text(
                        item.label,
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