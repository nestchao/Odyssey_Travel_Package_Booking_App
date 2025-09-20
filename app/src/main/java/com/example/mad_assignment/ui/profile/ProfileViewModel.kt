package com.example.mad_assignment.ui.profile

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
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error(
                message = "User not authenticated"
            )
            return
        }

        _uiState.value = ProfileUiState.Loading()

        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(currentUser.uid)
                if (user != null) {
                    _uiState.value = ProfileUiState.Success(user = user)
                } else {
                    _uiState.value = ProfileUiState.Error(
                        message = "User profile not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    message = "Failed to load profile: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun refreshProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.value = ProfileUiState.Error(
                message = "User not authenticated"
            )
            return
        }

        _uiState.update { currentState ->
            when (currentState) {
                is ProfileUiState.Success -> currentState.copy(isRefreshing = true)
                is ProfileUiState.Error -> ProfileUiState.Loading(isRefreshing = true)
                is ProfileUiState.Loading -> currentState.copy(isRefreshing = true)
            }
        }

        viewModelScope.launch {
            try {
                val user = userRepository.getUserById(currentUser.uid)
                if (user != null) {
                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        isRefreshing = false
                    )
                } else {
                    _uiState.value = ProfileUiState.Error(
                        message = "User profile not found",
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    message = "Failed to refresh profile: ${e.message ?: "Unknown error"}",
                    isRefreshing = false
                )
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = ProfileUiState.Loading()
    }

    fun updateProfile(updates: Map<String, Any>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _uiState.value = ProfileUiState.Error("Cannot update: User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                val success = userRepository.updateUser(userId, updates)
                if (success) {
                    // Re-fetch user data from the source of truth to ensure consistency.
                    val updatedUser = userRepository.getUserById(userId)
                    if (updatedUser != null) {
                        _uiState.value = ProfileUiState.Success(user = updatedUser)
                    } else {
                        _uiState.value = ProfileUiState.Error(message = "Could not refresh profile after update.")
                    }
                } else {
                    _uiState.value = ProfileUiState.Error(
                        message = "Failed to update profile.",
                        user = (uiState.value as? ProfileUiState.Success)?.user
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(
                    message = "Failed to update profile: ${e.message ?: "Unknown error"}",
                    user = (uiState.value as? ProfileUiState.Success)?.user
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { currentState ->
            when (currentState) {
                is ProfileUiState.Error -> {
                    if (currentState.user != null) {
                        ProfileUiState.Success(user = currentState.user)
                    } else {
                        ProfileUiState.Loading()
                    }
                }
                else -> currentState
            }
        }
    }
}