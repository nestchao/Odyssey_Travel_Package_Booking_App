package com.example.mad_assignment.ui.booking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.BookingType
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.repository.BookingRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.collections.forEach

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val travelPackageRepository: TravelPackageRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private const val TAG = "BookingViewModel"
    }

    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Loading)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading

            try {
                // Get current user ID from Firebase Auth
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _uiState.value = BookingUiState.Error("User not authenticated")
                    return@launch
                }

                val bookingsResult = bookingRepository.getBookingsByUserId(userId)
                if (bookingsResult.isFailure) {
                    _uiState.value = BookingUiState.Error(
                        bookingsResult.exceptionOrNull()?.message ?: "Failed to load bookings"
                    )
                    return@launch
                }

                val bookings = bookingsResult.getOrNull() ?: emptyList()

                if (bookings.isEmpty()) {
                    _uiState.value = BookingUiState.Empty
                    return@launch
                }

                // Categorize bookings by type
                val categorizedBookings = categorizeBookingsByDate(bookings)

                // Load package details for all bookings
                val packageIds = bookings.map { it.packageId }.distinct()
                val packagesMap = loadPackageDetails(packageIds)

                _uiState.value = BookingUiState.Success(
                    currentBookings = categorizedBookings.current,
                    upcomingBookings = categorizedBookings.upcoming,
                    pastBookings = categorizedBookings.past,
                    packages = packagesMap
                )

                Log.d(TAG, "Loaded ${bookings.size} bookings successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading bookings", e)
                _uiState.value = BookingUiState.Error(
                    e.message ?: "An unexpected error occurred"
                )
            }
        }
    }

    fun refreshBookings() {
        loadBookings()
    }

    fun showBookingDetails(bookingId: String) {
        val currentState = _uiState.value
        if (currentState is BookingUiState.Success) {
            _uiState.value = currentState.copy(
                showBookingDetails = true,
                selectedBookingId = bookingId
            )
        }
    }

    fun hideBookingDetails() {
        val currentState = _uiState.value
        if (currentState is BookingUiState.Success) {
            _uiState.value = currentState.copy(
                showBookingDetails = false,
                selectedBookingId = null
            )
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                val result = bookingRepository.cancelBooking(bookingId)

                if (result.isSuccess) {
                    Log.d(TAG, "Booking $bookingId cancelled successfully")
                    // Refresh bookings to reflect the cancellation
                    refreshBookings()
                } else {
                    Log.e(TAG, "Failed to cancel booking: ${result.exceptionOrNull()?.message}")
                    // You might want to show an error message to the user here
                    // For now, we'll just refresh to show current state
                    refreshBookings()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling booking $bookingId", e)
                // Refresh bookings anyway to show current state
                refreshBookings()
            }
        }
    }

    private suspend fun loadPackageDetails(packageIds: List<String>): Map<String, TravelPackageWithImages?> {
        val packagesMap = mutableMapOf<String, TravelPackageWithImages?>()

        packageIds.forEach { packageId ->
            try {
                val packageResult = travelPackageRepository.getPackageWithImages(packageId)
                packagesMap[packageId] = packageResult
            } catch (e: Exception) {
                Log.e(TAG, "Error loading package $packageId", e)
                packagesMap[packageId] = null
            }
        }

        return packagesMap
    }

    private fun categorizeBookingsByDate(bookings: List<Booking>): CategorizedBookings {
        val now = Date()
        val calendar = Calendar.getInstance()

        // Set calendar to start of today for more accurate comparison
        calendar.time = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.time

        val current = mutableListOf<Booking>()
        val upcoming = mutableListOf<Booking>()
        val past = mutableListOf<Booking>()

        bookings.forEach { booking ->
            // Skip cancelled bookings from categorization by date
            if (booking.status == BookingStatus.CANCELLED || booking.status == BookingStatus.REFUNDED) {
                // Add cancelled bookings to past for record keeping
                past.add(booking.copy(bookingType = BookingType.PAST))
                return@forEach
            }

            val startDate = booking.startBookingDate?.toDate()
            val endDate = booking.endBookingDate?.toDate()

            when {
                startDate == null || endDate == null -> {
                    // If dates are missing, categorize based on status
                    when (booking.status) {
                        BookingStatus.COMPLETED -> past.add(booking.copy(bookingType = BookingType.PAST))
                        else -> upcoming.add(booking.copy(bookingType = BookingType.UPCOMING))
                    }
                }

                endDate.before(todayStart) -> {
                    // Trip has ended
                    past.add(booking.copy(bookingType = BookingType.PAST))
                }

                startDate.after(todayStart) -> {
                    // Trip hasn't started yet
                    upcoming.add(booking.copy(bookingType = BookingType.UPCOMING))
                }

                else -> {
                    // Trip is ongoing (start date is today or in the past, end date is today or in the future)
                    current.add(booking.copy(bookingType = BookingType.CURRENT))
                }
            }
        }

        // Sort bookings within each category
        return CategorizedBookings(
            current = current.sortedBy { it.startBookingDate },
            upcoming = upcoming.sortedBy { it.startBookingDate },
            past = past.sortedByDescending { it.endBookingDate ?: it.createdAt }
        )
    }

    private data class CategorizedBookings(
        val current: List<Booking>,
        val upcoming: List<Booking>,
        val past: List<Booking>
    )
}