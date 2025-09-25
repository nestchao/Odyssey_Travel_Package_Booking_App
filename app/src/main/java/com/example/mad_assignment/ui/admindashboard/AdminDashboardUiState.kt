package com.example.mad_assignment.ui.admindashboard

import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType

data class DashboardStats(
    val totalUsers: Int = 0,
    val totalBookings: Int = 0,
    val totalRevenue: Double = 0.0,
    val activeUsers: Int = 0,
    val newUsersToday: Int = 0,
    val pendingBookings: Int = 0
)

data class RecentActivity(
    val id: String,
    val type: ActivityType,
    val description: String,
    val timestamp: String,
    val userId: String? = null,
    val bookingId: String? = null
)

enum class ActivityType {
    USER_REGISTRATION,
    BOOKING_CREATED,
    BOOKING_CANCELLED,
    PAYMENT_COMPLETED,
    USER_UPDATED
}

sealed interface AdminDashboardUiState {
    data class Loading(
        val isRefreshing: Boolean = false
    ) : AdminDashboardUiState

    data class Success(
        val currentUser: User,
        val stats: DashboardStats,
        val recentActivity: List<RecentActivity>,
        val isRefreshing: Boolean = false
    ) : AdminDashboardUiState {
        val isAdmin: Boolean get() = currentUser.userType == UserType.ADMIN
    }

    data class Error(
        val message: String,
        val currentUser: User? = null
    ) : AdminDashboardUiState
}