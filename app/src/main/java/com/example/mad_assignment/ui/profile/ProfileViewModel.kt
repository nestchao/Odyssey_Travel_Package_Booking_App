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

    fun loadProfile(forceServer: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading()
            try {
                val userId = auth.currentUser?.uid
                    ?: throw IllegalStateException("User is not authenticated.")

                // This now passes the 'forceServer' parameter to the repository
                val user = userRepository.getUserById(userId, forceServer = forceServer)
                    ?: throw IllegalStateException("User profile not found in database.")

                // Do the same for your profile picture repository
                val profilePic = profilePictureRepository.getProfilePicture(userId, forceServer = forceServer)
                    ?: throw IllegalStateException("Profile picture not found in database.")

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