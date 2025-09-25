package com.example.mad_assignment.ui.admindashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    onNavigateToUsers: () -> Unit = {},
    onNavigateToBookings: () -> Unit = {},
    onNavigateToPackage: () -> Unit = {},
    onNavigateToPayment: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = (uiState as? AdminDashboardUiState.Success)?.isRefreshing ?: false,
        onRefresh = { viewModel.refreshDashboard() }
    )

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        when (val state = uiState) {
            is AdminDashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AdminDashboardUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Access Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Red
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { viewModel.clearError() }) {
                            Text("Retry")
                        }
                        Button(onClick = {
                            viewModel.signOut()
                            onSignOut()
                        }) {
                            Text("Sign Out")
                        }
                    }
                }
            }

            is AdminDashboardUiState.Success -> {
                AdminDashboardContent(
                    state = state,
                    onNavigateToUsers = onNavigateToUsers,
                    onNavigateToBookings = onNavigateToBookings,
                    onNavigateToPackage = onNavigateToPackage,
                    onNavigateToPayment = onNavigateToPayment,
                    onNavigateToNotifications = onNavigateToNotifications,
                    onSignOut = {
                        viewModel.signOut()
                        onSignOut()
                    }
                )
            }
        }

        PullRefreshIndicator(
            refreshing = (uiState as? AdminDashboardUiState.Success)?.isRefreshing ?: false,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun AdminDashboardContent(
    state: AdminDashboardUiState.Success,
    onNavigateToUsers: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToPackage: () -> Unit,
    onNavigateToPayment: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(Color(0xFFF8F9FA))
    ) {
        item {
            // Header
            AdminHeader(
                adminName = "${state.currentUser.firstName} ${state.currentUser.lastName}",
                onSignOut = onSignOut
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Stats Overview
            Text(
                text = "Dashboard Overview",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getStatCards(state.stats)) { statCard ->
                    StatCard(
                        title = statCard.title,
                        value = statCard.value,
                        icon = statCard.icon,
                        color = statCard.color,
                        onClick = statCard.onClick
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getQuickActions(onNavigateToUsers, onNavigateToBookings, onNavigateToPackage, onNavigateToPayment,onNavigateToNotifications)) { action ->
                    QuickActionCard(
                        title = action.title,
                        icon = action.icon,
                        color = action.color,
                        onClick = action.onClick
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))

            // Recent Activity
            Text(
                text = "Recent Activity",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    state.recentActivity.take(5).forEach { activity ->
                        ActivityItem(
                            activity = activity,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminHeader(
    adminName: String,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = adminName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Admin Dashboard",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            IconButton(
                onClick = onSignOut,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = Icons.Filled.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = Color.White
                )
            }
        }
    }
}

data class StatCardData(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

private fun getStatCards(stats: DashboardStats): List<StatCardData> {
    return listOf(
        StatCardData(
            title = "Total Users",
            value = stats.totalUsers.toString(),
            icon = Icons.Filled.People,
            color = Color(0xFF10B981),
            onClick = {}
        ),
        StatCardData(
            title = "Total Bookings",
            value = stats.totalBookings.toString(),
            icon = Icons.Filled.BookOnline,
            color = Color(0xFF3B82F6),
            onClick = {}
        ),
        StatCardData(
            title = "Revenue",
            value = "RM${String.format("%.0f", stats.totalRevenue)}",
            icon = Icons.Filled.AttachMoney,
            color = Color(0xFF8B5CF6),
            onClick = {}
        ),
        StatCardData(
            title = "Active Users",
            value = stats.activeUsers.toString(),
            icon = Icons.Filled.TrendingUp,
            color = Color(0xFFF59E0B),
            onClick = {}
        )
    )
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class QuickActionData(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

private fun getQuickActions(
    onNavigateToUsers: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit
): List<QuickActionData> {
    return listOf(
        QuickActionData(
            title = "Manage Users",
            icon = Icons.Filled.People,
            color = Color(0xFF10B981),
            onClick = onNavigateToUsers
        ),
        QuickActionData(
            title = "Manage Bookings",

            icon = Icons.Filled.BookOnline,
            color = Color(0xFF3B82F6),
            onClick = onNavigateToBookings
        ),
        QuickActionData(
            title = "Manage Packages",
            icon = Icons.Filled.Inventory2,
            color = Color(0xFF8B5CF6),
            onClick = onNavigateToAnalytics
        ),
        QuickActionData(
            title = "Manage Payment",
            icon = Icons.Filled.Payment,
            color = Color(0xFF6B7280),
            onClick = onNavigateToSettings
        ) ,
        QuickActionData(
        title = "Notification",
        icon = Icons.Filled.Notifications,
        color = Color(0xFFF59E0B),
        onClick = onNavigateToNotifications
        )
    )
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 10.sp,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActivityItem(
    activity: RecentActivity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = when (activity.type) {
            ActivityType.USER_REGISTRATION -> Icons.Filled.PersonAdd to Color(0xFF10B981)
            ActivityType.BOOKING_CREATED -> Icons.Filled.BookOnline to Color(0xFF3B82F6)
            ActivityType.BOOKING_CANCELLED -> Icons.Filled.Cancel to Color(0xFFEF4444)
            ActivityType.PAYMENT_COMPLETED -> Icons.Filled.Payment to Color(0xFF8B5CF6)
            ActivityType.USER_UPDATED -> Icons.Filled.Edit to Color(0xFFF59E0B)
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.description,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = activity.timestamp,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}