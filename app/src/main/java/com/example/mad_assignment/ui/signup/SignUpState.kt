package com.example.mad_assignment.ui.signup

import com.example.mad_assignment.data.model.User

sealed interface SignUpUiState {
    data class Idle(
        val name: String = "",
        val email: String = "",
        val phoneNumber: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val isPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false,
        val nameError: String? = null,
        val emailError: String? = null,
        val phoneNumberError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    ) : SignUpUiState

    data class Loading(
        val name: String = "",
        val email: String = "",
        val phoneNumber: String = ""
    ) : SignUpUiState

    data class Error(
        val message: String,
        val name: String = "",
        val email: String = "",
        val phoneNumber: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val isPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false,
        val nameError: String? = null,
        val emailError: String? = null,
        val phoneNumberError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null
    ) : SignUpUiState

    data class Success(
        val user: User
    ) : SignUpUiState
}