package com.example.mad_assignment.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.util.toDataUri

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToAccountDetails: () -> Unit = {},
    onNavigateToWishlist: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAllBookings: () -> Unit = {},
    onNavigateToUpcomingBookings: () -> Unit = {},
    onNavigateToRecentlyViewed: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LoadingScreen()
            }
            is ProfileUiState.Error -> {
                if (state.user != null && state.ProfilePic != null) {
                    ProfileContent(
                        uiState = ProfileUiState.Success(
                            user = state.user,
                            ProfilePic = state.ProfilePic
                        ),
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

                    LaunchedEffect(state.message) {
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
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState.Success,
    onNavigateToAccountDetails: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAllBookings: () -> Unit,
    onNavigateToUpcomingBookings: () -> Unit,
    onNavigateToRecentlyViewed: () -> Unit,
    onSignOut: () -> Unit
) {
    val scrollState = rememberScrollState()
    val imageBitmap = base64ToImageBitmap(uiState.ProfilePic.profilePictureBase64)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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
                // Profile Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Default Profile Icon",
                                tint = Color.Gray,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Username with character limit
                    Text(
                        text = uiState.shortDisplayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        maxLines = 1
                    )

                    // Show truncated indicator if name is too long
                    if (uiState.isNameTruncated) {
                        Text(
                            text = "...",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.offset(y = (-4).dp)
                        )
                    }
                }

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
        modifier = Modifier.width(80.dp)
    ) {
        Card(
            onClick = onClick,
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = title,
            fontSize = 11.sp,
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

fun base64ToImageBitmap(base64String: String?): ImageBitmap? {
    val decodedBytes = toDataUri(base64String) ?: return null

    return try {
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size).asImageBitmap()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}