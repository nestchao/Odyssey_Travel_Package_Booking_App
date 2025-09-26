package com.example.mad_assignment.ui.admindashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.ActivityRepository
import com.example.mad_assignment.data.repository.BookingRepository
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val activityRepository: ActivityRepository,
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

                    val (stats, activities) = coroutineScope {
                        val statsDeferred = async { loadDashboardStats() }
                        val activitiesDeferred = async { loadRecentActivities() }
                        statsDeferred.await() to activitiesDeferred.await()
                    }

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
        return coroutineScope {
            val totalUsersDeferred = async { userRepository.countAllActiveUsers() }
            val newUsersTodayDeferred = async { userRepository.countNewUsersToday() }
            val totalBookingsDeferred = async { bookingRepository.countAllBookings() }
            val pendingBookingsDeferred = async { bookingRepository.countPendingBookings() }
            val totalRevenueDeferred = async { bookingRepository.getTotalRevenue() }

            DashboardStats(
                totalUsers = totalUsersDeferred.await().toInt(),
                newUsersToday = newUsersTodayDeferred.await().toInt(),
                totalBookings = totalBookingsDeferred.await().toInt(),
                pendingBookings = pendingBookingsDeferred.await().toInt(),
                totalRevenue = totalRevenueDeferred.await(),
                activeUsers = totalUsersDeferred.await().toInt()
            )
        }
    }

    private suspend fun loadRecentActivities(): List<RecentActivity> {
        val activitiesFromRepo = activityRepository.getRecentActivities()

        return activitiesFromRepo.map { activity ->
            RecentActivity(
                id = activity.id,
                type = ActivityType.valueOf(activity.type.name),
                description = activity.description,
                timestamp = formatTimestamp(activity.timestamp)
            )
        }
    }

    private fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"
        val now = System.currentTimeMillis()
        val time = timestamp.toDate().time
        val diff = now - time

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            else -> "$days days ago"
        }
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
