package com.example.mad_assignment.ui.forgetpassword

sealed interface ForgotPasswordUiState {
    data class Idle(
        val email: String = "",
        val emailError: String? = null
    ) : ForgotPasswordUiState

    data class Loading(
        val email: String = ""
    ) : ForgotPasswordUiState

    data class Error(
        val message: String,
        val email: String = "",
        val emailError: String? = null
    ) : ForgotPasswordUiState

    object Success : ForgotPasswordUiState
}