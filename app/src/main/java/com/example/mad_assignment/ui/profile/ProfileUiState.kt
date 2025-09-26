package com.example.mad_assignment.ui.profile

import com.example.mad_assignment.data.model.ProfilePic
import com.example.mad_assignment.data.model.User

sealed interface ProfileUiState {
    data class Loading(
        val ProfilePic: ProfilePic? = null
    ) : ProfileUiState

    data class Success(
        val user: User,
        val ProfilePic: ProfilePic,
        val totalTrips: Int,
        val yearsOnOdyssey: Int
    ) : ProfileUiState {
        val displayName: String
            get() {
                val fullName = "${user.firstName} ${user.lastName}".trim()
                return fullName.ifEmpty { user.userEmail }
            }
        val shortDisplayName: String get() = displayName.take(12)
        val isNameTruncated: Boolean get() = displayName.length > 12

    }

    data class Error(
        val message: String,
        val user: User? = null,
        val ProfilePic: ProfilePic? = null
    ) : ProfileUiState
}

