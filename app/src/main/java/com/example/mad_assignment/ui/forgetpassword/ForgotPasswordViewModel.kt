package com.example.mad_assignment.ui.forgetpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is ForgotPasswordUiState.Idle -> currentState.copy(email = email, emailError = null)
                is ForgotPasswordUiState.Error -> currentState.copy(email = email, emailError = null)
                else -> currentState
            }
        }
    }

    fun resetPassword() {
        val currentState = _uiState.value
        val email = when (currentState) {
            is ForgotPasswordUiState.Idle -> currentState.email
            is ForgotPasswordUiState.Error -> currentState.email
            else -> return
        }

        if (!validateEmail(email)) return

        viewModelScope.launch {
            _uiState.value = ForgotPasswordUiState.Loading(email = email)

            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                _uiState.value = ForgotPasswordUiState.Success
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState.Error(
                    message = getResetErrorMessage(e),
                    email = email
                )
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        val emailRegex = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )

        val emailError = when {
            email.isBlank() -> "Email is required"
            !emailRegex.matcher(email).matches() -> "Invalid email format"
            else -> null
        }

        if (emailError != null) {
            _uiState.value = ForgotPasswordUiState.Error(
                message = "Please fix the errors",
                email = email,
                emailError = emailError
            )
            return false
        }
        return true
    }

    private fun getResetErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The email address is badly formatted." -> "Invalid email format"
            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                "No account found with this email"
            else -> "Password reset failed: ${exception.message ?: "Unknown error"}"
        }
    }
}