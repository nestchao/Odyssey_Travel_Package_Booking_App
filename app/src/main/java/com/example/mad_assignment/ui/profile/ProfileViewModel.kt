package com.example.mad_assignment.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.ProfilePicRepository
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
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profilePictureRepository: ProfilePicRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()


    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading()
            try {
                // Get the user ID first
                val userId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User is not authenticated.")

                // Fetch the user, and throw an error if it's not found
                val user = userRepository.getUserById(userId)
                    ?: throw IllegalStateException("User profile not found in database.")

                // Fetch the profile picture, and throw an error if it's not found
                val profilePic = profilePictureRepository.getProfilePicture(userId)
                    ?: throw IllegalStateException("Profile picture not found in database.")

                // Only create the Success state if BOTH were found successfully
                _uiState.value = ProfileUiState.Success(user, profilePic)

            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    fun updateUserProfilePicture(base64Data: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is ProfileUiState.Success) {
                try {
                    // Get the userID directly from the 'user' object in the state
                    val userId = currentState.user.userID
                    val success = profilePictureRepository.setProfilePicture(userId, base64Data)

                    if (success) {
                        // Reload the whole profile to ensure data is consistent
                        loadProfile()
                    } else {
                        // Pass the old data along with the error
                        _uiState.value = ProfileUiState.Error(
                            message = "Failed to save picture.",
                            user = currentState.user,
                            ProfilePic = currentState.ProfilePic
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = ProfileUiState.Error("An error occurred while saving.")
                }
            }
        }
    }



    fun signOut() {
        auth.signOut()
        _uiState.value = ProfileUiState.Loading()
    }

    fun updateProfile(updates: Map<String, Any>) {
        val userId = auth.currentUser?.uid ?: return // Early exit if not logged in

        val originalState = _uiState.value as? ProfileUiState.Success

        viewModelScope.launch {
            try {
                val success = userRepository.updateUser(userId, updates)
                if (success) {
                    // The best practice is to reload everything from the source of truth.
                    loadProfile()
                } else {
                    _uiState.value = ProfileUiState.Error(
                        message = "Failed to update profile.",
                        user = originalState?.user,
                        ProfilePic = originalState?.ProfilePic
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    message = "Failed to update profile: ${e.message ?: "Unknown error"}",
                    user = originalState?.user,
                    ProfilePic = originalState?.ProfilePic
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { currentState ->
            if (currentState is ProfileUiState.Error) {
                // If we have old data, we can show a loading state that holds that old data
                // to prevent the screen from blinking empty.
                ProfileUiState.Loading(ProfilePic = currentState.ProfilePic)
            } else {
                currentState
            }
        }
    }
}