package com.example.mad_assignment.ui.manageuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Activity
import com.example.mad_assignment.data.model.ActivityType
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import com.example.mad_assignment.data.repository.ActivityRepository
import com.example.mad_assignment.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageUserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository
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


    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                onDismissDialog() // Dismiss dialog for snappy UI
                if (userRepository.deleteUser(userId)) {
                    loadUsers() // Reload the list after deletion
                } else {
                    _uiState.value = ManageUserUiState.Error("Failed to delete user.")
                }
            } catch (e: Exception) {
                _uiState.value = ManageUserUiState.Error("Failed to delete user: ${e.message}")
            }
        }
    }

    fun onEditUserClicked(user: User) {
        val currentState = _uiState.value
        if (currentState is ManageUserUiState.Success) {
            _uiState.value = currentState.copy(showEditDialog = true, selectedUser = user)
        }
    }

    fun onDeleteUserClicked(user: User) {
        val currentState = _uiState.value
        if (currentState is ManageUserUiState.Success) {
            _uiState.value = currentState.copy(showDeleteDialog = true, selectedUser = user)
        }
    }

    fun onDismissDialog() {
        val currentState = _uiState.value
        if (currentState is ManageUserUiState.Success) {
            _uiState.value = currentState.copy(
                showEditDialog = false,
                showDeleteDialog = false,
                selectedUser = null
            )
        }
    }

    fun updateUserInfo(user: User) {
        viewModelScope.launch {
            try {
                onDismissDialog() // Dismiss dialog for snappy UI
                val userUpdates = mapOf(
                    "gender" to user.gender.name,
                    "firstName" to user.firstName,
                    "lastName" to user.lastName,
                    "userPhoneNumber" to user.userPhoneNumber,
                    "userType" to user.userType.name
                )
                if (userRepository.updateUser(user.userID, userUpdates)) {
                    loadUsers()
                    val activity = Activity(
                        description = "User profile updated for ${user.userEmail}",
                        type = ActivityType.USER_UPDATED,
                        userId = user.userID
                    )
                    activityRepository.createActivity(activity)
                } else {
                    _uiState.value = ManageUserUiState.Error("Failed to update user.")
                }
            } catch (e: Exception) {
                _uiState.value = ManageUserUiState.Error("An error occurred: ${e.message}")
            }
        }
    }
}