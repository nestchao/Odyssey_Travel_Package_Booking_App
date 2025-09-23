package com.example.mad_assignment.ui.accountdetail

import android.net.Uri
import com.example.mad_assignment.data.model.ProfilePic
import com.example.mad_assignment.data.model.User

sealed interface AccountDetailsUiState {
    data class Loading(
        val isSaving: Boolean = false
    ) : AccountDetailsUiState

    data class Success(
        val user: User,
        val profilePic: ProfilePic,
        val newImagePreview: String? = null,
        val isSaving: Boolean = false,
        val firstNameError: String? = null,
        val lastNameError: String? = null,
        val emailError: String? = null,
        val phoneNumberError: String? = null
    ) : AccountDetailsUiState {
        val isFormValid: Boolean get() =
            firstNameError == null &&
                    lastNameError == null &&
                    emailError == null &&
                    phoneNumberError == null &&
                    user.firstName.isNotBlank() &&
                    user.lastName.isNotBlank() &&
                    user.userEmail.isNotBlank()
    }

    data class Error(
        val message: String,
        val user: User? = null
    ) : AccountDetailsUiState
}