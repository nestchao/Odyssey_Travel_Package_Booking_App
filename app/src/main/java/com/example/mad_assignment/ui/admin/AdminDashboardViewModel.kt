package com.example.mad_assignment.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = AdminDashboardUiState.Error(
                message = "User not authenticated"
            )
            return
        }

        _uiState.value = AdminDashboardUiState.Loading()

        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(currentUser.uid)
                if (user != null && user.userType == com.example.mad_assignment.data.model.UserType.ADMIN) {
                    val stats = loadDashboardStats()
                    val activities = loadRecentActivities()

                    _uiState.value = AdminDashboardUiState.Success(
                        currentUser = user,
                        stats = stats,
                        recentActivity = activities
                    )
                } else {
                    _uiState.value = AdminDashboardUiState.Error(
                        message = "Access denied. Admin privileges required."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AdminDashboardUiState.Error(
                    message = "Failed to load dashboard: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun refreshDashboard() {
        val currentState = _uiState.value
        if (currentState !is AdminDashboardUiState.Success) return

        _uiState.update { currentState.copy(isRefreshing = true) }

        viewModelScope.launch {
            try {
                val stats = loadDashboardStats()
                val activities = loadRecentActivities()

                _uiState.update {
                    currentState.copy(
                        stats = stats,
                        recentActivity = activities,
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AdminDashboardUiState.Error(
                    message = "Failed to refresh dashboard: ${e.message ?: "Unknown error"}",
                    currentUser = currentState.currentUser
                )
            }
        }
    }

    private suspend fun loadDashboardStats(): DashboardStats {
        // In a real app, you would fetch this from your repositories
        return DashboardStats(
            totalUsers = 1250,
            totalBookings = 3420,
            totalRevenue = 125000.50,
            activeUsers = 892,
            newUsersToday = 23,
            pendingBookings = 45
        )
    }

    private suspend fun loadRecentActivities(): List<RecentActivity> {
        // In a real app, you would fetch this from your repositories
        return listOf(
            RecentActivity(
                id = "1",
                type = ActivityType.USER_REGISTRATION,
                description = "New user registered: john.doe@email.com",
                timestamp = "2 minutes ago"
            ),
            RecentActivity(
                id = "2",
                type = ActivityType.BOOKING_CREATED,
                description = "New booking created for Bali Package",
                timestamp = "5 minutes ago"
            ),
            RecentActivity(
                id = "3",
                type = ActivityType.PAYMENT_COMPLETED,
                description = "Payment completed: $2,350.00",
                timestamp = "12 minutes ago"
            ),
            RecentActivity(
                id = "4",
                type = ActivityType.BOOKING_CANCELLED,
                description = "Booking cancelled: Japan Tour",
                timestamp = "1 hour ago"
            ),
            RecentActivity(
                id = "5",
                type = ActivityType.USER_UPDATED,
                description = "User profile updated",
                timestamp = "2 hours ago"
            )
        )
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = AdminDashboardUiState.Loading()
    }

    fun clearError() {
        _uiState.update { currentState ->
            when (currentState) {
                is AdminDashboardUiState.Error -> AdminDashboardUiState.Loading()
                else -> currentState
            }
        }
    }
}
