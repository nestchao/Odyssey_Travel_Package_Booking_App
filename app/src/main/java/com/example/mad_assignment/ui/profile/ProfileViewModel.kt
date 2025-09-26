package com.example.mad_assignment.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.BookingRepository
import com.example.mad_assignment.data.repository.ProfilePicRepository
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profilePictureRepository: ProfilePicRepository,
    private val bookingRepository: BookingRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfileData()
    }

    fun retry() {
        observeProfileData()
    }

    private fun observeProfileData() {
        _uiState.value = ProfileUiState.Loading()

        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                _uiState.value = ProfileUiState.Error("User is not authenticated.")
                return@launch
            }

            val userStream = userRepository.getUserStream(userId)
            val profilePicStream = profilePictureRepository.getProfilePictureStream(userId)
            val totalTripsStream = flow { emit(bookingRepository.getTotalTrips(userId)) }

            combine(userStream, profilePicStream, totalTripsStream) { userResult, profilePicResult, totalTripsResult ->
                if (userResult.isSuccess && profilePicResult.isSuccess && totalTripsResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    val profilePic = profilePicResult.getOrThrow()
                    val totalTrips = totalTripsResult.getOrThrow()
                    val yearsOnOdyssey = calculateYearsOnOdyssey(user.createdAt?.toDate())

                    ProfileUiState.Success(
                        user = user,
                        ProfilePic = profilePic,
                        totalTrips = totalTrips,
                        yearsOnOdyssey = yearsOnOdyssey
                    )
                } else {
                    val errorMessage = userResult.exceptionOrNull()?.message
                        ?: profilePicResult.exceptionOrNull()?.message
                        ?: totalTripsResult.exceptionOrNull()?.message // Now this reference is resolved
                        ?: "Failed to load profile data."
                    ProfileUiState.Error(errorMessage)
                }
            }.catch { e ->
                emit(ProfileUiState.Error("An unexpected error occurred: ${e.message}"))
            }.collect { combinedState ->
                _uiState.value = combinedState
            }
        }
    }

    private fun calculateYearsOnOdyssey(creationDate: Date?): Int {
        if (creationDate == null) return 0

        val startCal = Calendar.getInstance()
        startCal.time = creationDate

        val endCal = Calendar.getInstance() // Gets current date

        var years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)

        if (endCal.get(Calendar.DAY_OF_YEAR) < startCal.get(Calendar.DAY_OF_YEAR)) {
            years--
        }

        return years.coerceAtLeast(0)
    }


    fun signOut() {
        auth.signOut()
        _uiState.value = ProfileUiState.Loading()
    }

    fun clearError() {
        _uiState.update { currentState ->
            if (currentState is ProfileUiState.Error) {
                ProfileUiState.Loading(ProfilePic = currentState.ProfilePic)
            } else {
                currentState
            }
        }
    }
}