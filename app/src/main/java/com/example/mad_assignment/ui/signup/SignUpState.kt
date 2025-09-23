package com.example.mad_assignment.ui.signup

import com.example.mad_assignment.data.model.User

sealed interface SignUpUiState {
    data class Idle(
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val phoneNumber: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val isPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false,
        val firstNameError: String? = null,
        val lastNameError: String? = null,
        val emailError: String? = null,
        val phoneNumberError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    ) : SignUpUiState

    data class Loading(
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val phoneNumber: String = ""
    ) : SignUpUiState

    data class Error(
        val message: String,
        val firstName: String = "",
        val lastName: String = "",
        val email: String = "",
        val phoneNumber: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val isPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false,
        val firstNameError: String? = null,
        val lastNameError: String? = null,
        val emailError: String? = null,
        val phoneNumberError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    ) : SignUpUiState

    data class Success(
        val user: User
    ) : SignUpUiState
}