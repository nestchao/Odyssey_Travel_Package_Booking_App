package com.example.mad_assignment.ui.manageuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import com.example.mad_assignment.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageUserUiState>(ManageUserUiState.Loading())
    val uiState: StateFlow<ManageUserUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _uiState.value = ManageUserUiState.Loading()
                val users = userRepository.getAllUsers()
                _uiState.value = ManageUserUiState.Success(users = users)
            } catch (e: Exception) {
                _uiState.value = ManageUserUiState.Error("Failed to load users: ${e.message}")
            }
        }
    }

    fun filterUsers(userType: UserType?) {
        val currentState = _uiState.value
        if (currentState is ManageUserUiState.Success) {
            _uiState.value = currentState.copy(selectedFilter = userType)
        }
    }

    fun updateUserType(userId: String, newType: UserType) {
        viewModelScope.launch {
            try {
                userRepository.updateUserType(userId, newType)
                loadUsers() // Reload the list after update
            } catch (e: Exception) {
                _uiState.value = ManageUserUiState.Error("Failed to update user: ${e.message}")
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                userRepository.deleteUser(userId)
                loadUsers() // Reload the list after deletion
            } catch (e: Exception) {
                _uiState.value = ManageUserUiState.Error("Failed to delete user: ${e.message}")
            }
        }
    }

    fun updateUserInfo(user: User) {
        viewModelScope.launch {
            try {

                val userUpdates = mapOf(
                    "gender" to user.gender.name,
                    "firstName" to user.firstName,
                    "lastName" to user.lastName,
                    "userEmail" to user.userEmail,
                    "userType" to user.userType.name
                )

                val wasSuccessful = userRepository.updateUser(user.userID, userUpdates)

                if (wasSuccessful) {
                    loadUsers()
                } else {
                    _uiState.value =
                        ManageUserUiState.Error("Failed to update user. Please try again.")
                }

            } catch (e: Exception) {

                _uiState.value = ManageUserUiState.Error("An error occurred: ${e.message}")
            }
        }
    }
}