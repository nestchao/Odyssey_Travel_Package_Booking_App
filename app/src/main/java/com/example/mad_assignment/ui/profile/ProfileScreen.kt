package com.example.mad_assignment.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToBooking: () -> Unit = {},
    onNavigateToAccountDetails: () -> Unit = {},
    onNavigateToWishlist: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAllBookings: () -> Unit = {},
    onNavigateToUpcomingBookings: () -> Unit = {},
    onNavigateToRecentlyViewed: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = when (val state = uiState) {
            is ProfileUiState.Success -> state.isRefreshing
            is ProfileUiState.Error -> state.isRefreshing
            is ProfileUiState.Loading -> state.isRefreshing
        },
        onRefresh = { viewModel.refreshProfile() }
    )

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                if (!state.isRefreshing) {
                    LoadingScreen()
                }
            }
            is ProfileUiState.Error -> {
                if (state.user != null) {
                    // Show profile with error snackbar
                    ProfileContent(
                        uiState = ProfileUiState.Success(user = state.user),
                        onNavigateToHome = onNavigateToHome,
                        onNavigateToBooking = onNavigateToBooking,
                        onNavigateToAccountDetails = onNavigateToAccountDetails,
                        onNavigateToWishlist = onNavigateToWishlist,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToAllBookings = onNavigateToAllBookings,
                        onNavigateToUpcomingBookings = onNavigateToUpcomingBookings,
                        onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
                        onSignOut = {
                            viewModel.signOut()
                            onSignOut()
                        }
                    )

                    // Show error snackbar
                    LaunchedEffect(state.message) {
                        // You can show a snackbar here
                        // snackbarHostState.showSnackbar(state.message)
                        viewModel.clearError()
                    }
                } else {
                    ErrorScreen(
                        error = state.message,
                        onRetry = { viewModel.loadProfile() }
                    )
                }
            }
            is ProfileUiState.Success -> {
                ProfileContent(
                    uiState = state,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToBooking = onNavigateToBooking,
                    onNavigateToAccountDetails = onNavigateToAccountDetails,
                    onNavigateToWishlist = onNavigateToWishlist,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAllBookings = onNavigateToAllBookings,
                    onNavigateToUpcomingBookings = onNavigateToUpcomingBookings,
                    onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
                    onSignOut = {
                        viewModel.signOut()
                        onSignOut()
                    }
                )
            }
        }

        PullRefreshIndicator(
            refreshing = when (val state = uiState) {
                is ProfileUiState.Success -> state.isRefreshing
                is ProfileUiState.Error -> state.isRefreshing
                is ProfileUiState.Loading -> state.isRefreshing
            },
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState.Success,
    onNavigateToHome: () -> Unit,
    onNavigateToBooking: () -> Unit,
    onNavigateToAccountDetails: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAllBookings: () -> Unit,
    onNavigateToUpcomingBookings: () -> Unit,
    onNavigateToRecentlyViewed: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Profile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Profile Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image Placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Username
                Text(
                    text = uiState.displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )

                // Stats Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileStat(number = uiState.totalTrips.toString(), label = "Trips")
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileStat(number = uiState.totalReviews.toString(), label = "Review")
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileStat(number = uiState.yearsOnOdyssey.toString(), label = "Years On Odyssey")
                }
            }
        }

        // Quick Actions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionItem(
                    icon = Icons.Filled.List,
                    title = "All Bookings",
                    onClick = onNavigateToAllBookings
                )
                QuickActionItem(
                    icon = Icons.Filled.DateRange,
                    title = "Upcoming",
                    onClick = onNavigateToUpcomingBookings
                )
                QuickActionItem(
                    icon = Icons.Filled.Schedule,
                    title = "Recently View",
                    onClick = onNavigateToRecentlyViewed
                )
            }
        }

        // Menu Items
        MenuItemRow(
            icon = Icons.Filled.Person,
            title = "Account Details",
            onClick = onNavigateToAccountDetails
        )

        MenuItemRow(
            icon = Icons.Filled.FavoriteBorder,
            title = "Wishlist",
            onClick = onNavigateToWishlist
        )

        MenuItemRow(
            icon = Icons.Filled.Settings,
            title = "Setting",
            onClick = onNavigateToSettings
        )

        MenuItemRow(
            icon = Icons.Filled.ExitToApp,
            title = "Sign Out",
            onClick = onSignOut
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottom Navigation
        BottomNavigationBar(
            onNavigateToHome = onNavigateToHome,
            onNavigateToBooking = onNavigateToBooking
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun ProfileStat(number: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun MenuItemRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                color = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    onNavigateToHome: () -> Unit,
    onNavigateToBooking: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomNavItem(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = false,
                onClick = onNavigateToHome
            )
            BottomNavItem(
                icon = Icons.Filled.Book,
                label = "Booking",
                isSelected = false,
                onClick = onNavigateToBooking
            )
            BottomNavItem(
                icon = Icons.Filled.Person,
                label = "Profile",
                isSelected = true,
                onClick = { }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Color(0xFF6200EE).copy(alpha = 0.1f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF6200EE) else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Color(0xFF6200EE) else Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}