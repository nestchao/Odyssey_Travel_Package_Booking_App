package com.example.mad_assignment.ui.changepassword

sealed interface ChangePasswordUiState {
    data class Idle(
        val oldPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val isOldPasswordVisible: Boolean = false,
        val isNewPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false,
        val oldPasswordError: String? = null,
        val newPasswordError: String? = null,
        val confirmPasswordError: String? = null
    ) : ChangePasswordUiState {
        val isFormValid: Boolean get() =
            oldPasswordError == null &&
                    newPasswordError == null &&
                    confirmPasswordError == null &&
                    oldPassword.isNotBlank() &&
                    newPassword.isNotBlank() &&
                    confirmPassword.isNotBlank()
    }

    data class Loading(
        val oldPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val isOldPasswordVisible: Boolean = false,
        val isNewPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false
    ) : ChangePasswordUiState

    data class Success(
        val message: String = "Password changed successfully"
    ) : ChangePasswordUiState

    data class Error(
        val message: String? = null,
        val oldPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val isOldPasswordVisible: Boolean = false,
        val isNewPasswordVisible: Boolean = false,
        val isConfirmPasswordVisible: Boolean = false,
        val oldPasswordError: String? = null,
        val newPasswordError: String? = null,
        val confirmPasswordError: String? = null
    ) : ChangePasswordUiState
}