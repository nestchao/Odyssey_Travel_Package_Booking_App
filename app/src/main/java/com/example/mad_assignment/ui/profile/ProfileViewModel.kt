package com.example.mad_assignment.ui.profile

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.ProfilePicRepository
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profilePictureRepository: ProfilePicRepository,
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

            combine(userStream, profilePicStream) { userResult, profilePicResult ->
                if (userResult.isSuccess && profilePicResult.isSuccess) {
                    ProfileUiState.Success(
                        user = userResult.getOrThrow(),
                        ProfilePic = profilePicResult.getOrThrow()
                    )
                } else {
                    val errorMessage = userResult.exceptionOrNull()?.message
                        ?: profilePicResult.exceptionOrNull()?.message
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