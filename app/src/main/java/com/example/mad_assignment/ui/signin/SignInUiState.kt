package com.example.mad_assignment.ui.signin

import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType

sealed interface SignInUiState {
    data class Idle(
        val email: String = "",
        val password: String = "",
        val isPasswordVisible: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null
    ) : SignInUiState

    data class Loading(
        val email: String = "",
        val password: String = "",
        val isPasswordVisible: Boolean = false
    ) : SignInUiState

    data class Error(
        val message: String,
        val email: String = "",
        val password: String = "",
        val isPasswordVisible: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null
    ) : SignInUiState

    data class Success(
        val user: User,
        val email: String = "",
        val password: String = ""
    ) : SignInUiState {
        val isCustomer: Boolean get() = user.userType == UserType.CUSTOMER
        val isAdmin: Boolean get() = user.userType == UserType.ADMIN
        val displayName: String
            get() {
                val fullName = "${user.firstName} ${user.lastName}".trim()
                return fullName.ifEmpty { user.userEmail }
            }
    }
}