package com.example.mad_assignment.ui.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface Event {
    data class ShowToastAndNavigateBack(val message: String) : Event
}

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    fun updateOldPassword(password: String) {
        _uiState.update { currentState ->
            // Use a safe "if" check instead of an unsafe cast
            if (currentState is ChangePasswordUiState.Idle) {
                currentState.copy(
                    oldPassword = password,
                    oldPasswordError = null // Clear error on new input
                )
            } else {
                currentState // If not idle (e.g., loading), do nothing
            }
        }
    }

    fun updateNewPassword(password: String) {
        _uiState.update { currentState ->
            if (currentState is ChangePasswordUiState.Idle) {
                currentState.copy(
                    newPassword = password,
                    newPasswordError = validateNewPassword(password)
                )
            } else {
                currentState
            }
        }
    }

    fun updateConfirmPassword(password: String) {
        _uiState.update { currentState ->
            if (currentState is ChangePasswordUiState.Idle) {
                currentState.copy(
                    confirmPassword = password,
                    confirmPasswordError = validateConfirmPassword(currentState.newPassword, password)
                )
            } else {
                currentState
            }
        }
    }

    fun toggleOldPasswordVisibility() {
        _uiState.update { currentState ->
            when (currentState) {
                is ChangePasswordUiState.Idle -> currentState.copy(
                    isOldPasswordVisible = !currentState.isOldPasswordVisible
                )
                is ChangePasswordUiState.Error -> currentState.copy(
                    isOldPasswordVisible = !currentState.isOldPasswordVisible
                )
                is ChangePasswordUiState.Loading -> currentState.copy(
                    isOldPasswordVisible = !currentState.isOldPasswordVisible
                )
                else -> currentState
            }
        }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { currentState ->
            when (currentState) {
                is ChangePasswordUiState.Idle -> currentState.copy(
                    isNewPasswordVisible = !currentState.isNewPasswordVisible
                )
                is ChangePasswordUiState.Error -> currentState.copy(
                    isNewPasswordVisible = !currentState.isNewPasswordVisible
                )
                is ChangePasswordUiState.Loading -> currentState.copy(
                    isNewPasswordVisible = !currentState.isNewPasswordVisible
                )
                else -> currentState
            }
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { currentState ->
            when (currentState) {
                is ChangePasswordUiState.Idle -> currentState.copy(
                    isConfirmPasswordVisible = !currentState.isConfirmPasswordVisible
                )
                is ChangePasswordUiState.Error -> currentState.copy(
                    isConfirmPasswordVisible = !currentState.isConfirmPasswordVisible
                )
                is ChangePasswordUiState.Loading -> currentState.copy(
                    isConfirmPasswordVisible = !currentState.isConfirmPasswordVisible
                )
                else -> currentState
            }
        }
    }

    fun changePassword() {
        val currentState = _uiState.value
        val oldPassword = when (currentState) {
            is ChangePasswordUiState.Idle -> currentState.oldPassword
            is ChangePasswordUiState.Error -> currentState.oldPassword
            else -> return
        }
        val newPassword = when (currentState) {
            is ChangePasswordUiState.Idle -> currentState.newPassword
            is ChangePasswordUiState.Error -> currentState.newPassword
            else -> return
        }
        val confirmPassword = when (currentState) {
            is ChangePasswordUiState.Idle -> currentState.confirmPassword
            is ChangePasswordUiState.Error -> currentState.confirmPassword
            else -> return
        }

        if (!validateAllFields(oldPassword, newPassword, confirmPassword)) return

        _uiState.value = ChangePasswordUiState.Loading(
            oldPassword = oldPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user?.email != null) {
                    // Re-authenticate user with old password
                    val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)
                    user.reauthenticate(credential).await()

                    // Update password
                    user.updatePassword(newPassword).await()

                    _uiState.value = ChangePasswordUiState.Success()
                } else {
                    _uiState.value = ChangePasswordUiState.Error(
                        message = "User not authenticated",
                        oldPassword = oldPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword
                    )
                }
            } catch (e: Exception) {
                val errorMessage = getPasswordChangeErrorMessage(e)
                // Check if it's the specific error we care about
                if (errorMessage == "Old password is incorrect") {
                    viewModelScope.launch {
                        _events.emit(Event.ShowToastAndNavigateBack(errorMessage))
                    }
                } else {
                    _uiState.value = ChangePasswordUiState.Idle(
                        oldPassword = oldPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        oldPasswordError = errorMessage
                    )
                }
            }
        }
    }

    private fun validateAllFields(oldPassword: String, newPassword: String, confirmPassword: String): Boolean {
        val oldPasswordError = validateOldPassword(oldPassword)
        val newPasswordError = validateNewPassword(newPassword)
        val confirmPasswordError = validateConfirmPassword(newPassword, confirmPassword)

        if (oldPasswordError != null || newPasswordError != null || confirmPasswordError != null) {
            _uiState.update { currentState ->
                if (currentState is ChangePasswordUiState.Idle) { // Safe check
                    currentState.copy(
                        oldPasswordError = oldPasswordError,
                        newPasswordError = newPasswordError,
                        confirmPasswordError = confirmPasswordError
                    )
                } else {
                    currentState
                }
            }
            return false
        }
        return true
    }

    private fun validateOldPassword(password: String): String? {
        return when {
            password.isBlank() -> "Old password is required"
            else -> null
        }
    }

    private fun validateNewPassword(password: String): String? {
        return when {
            password.isBlank() -> "New password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    private fun validateConfirmPassword(newPassword: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Confirm password is required"
            confirmPassword != newPassword -> "Passwords do not match"
            else -> null
        }
    }

    private fun getPasswordChangeErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The password is invalid or the user does not have a password." -> "Old password is incorrect"
            "The given password is invalid. [ Password should be at least 6 characters ]" -> "Password must be at least 6 characters"
            else -> "Failed to change password: ${exception.message ?: "Unknown error"}"
        }
    }

}