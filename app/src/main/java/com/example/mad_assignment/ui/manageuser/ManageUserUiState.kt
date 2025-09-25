package com.example.mad_assignment.ui.manageuser

import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType

sealed interface ManageUserUiState {
    data class Loading(val isRefreshing: Boolean = false) : ManageUserUiState
    data class Success(
        val users: List<User>,
        val isRefreshing: Boolean = false,
        val selectedFilter: UserType? = null,
        val showEditDialog: Boolean = false,
        val selectedUser: User? = null
    ) : ManageUserUiState
    data class Error(val message: String, val users: List<User> = emptyList()) : ManageUserUiState
}