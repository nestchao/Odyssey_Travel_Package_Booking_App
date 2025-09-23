package com.example.mad_assignment.ui.accountdetail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.repository.UserRepository
import com.example.mad_assignment.data.model.Gender
import com.example.mad_assignment.data.model.ProfilePic
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.repository.ProfilePicRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class AccountDetailsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profilePictureRepository: ProfilePicRepository,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountDetailsUiState>(AccountDetailsUiState.Loading())
    val uiState: StateFlow<AccountDetailsUiState> = _uiState.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private var newProfilePicBase64: String? = null

    init {
        loadAccountDetails()
    }

    fun loadAccountDetails(forceServer: Boolean = false) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid
                    ?: throw Exception("User not authenticated.")

                // Fetch the user object
                val user = userRepository.getUserById(userId, forceServer = forceServer)
                    ?: throw Exception("User profile not found.")

                // Fetch the ProfilePic object
                val profilePic = profilePictureRepository.getProfilePicture(userId, forceServer = forceServer)
                // If no picture is found, create a default, empty ProfilePic object
                    ?: ProfilePic(userID = userId, profilePictureBase64 = null)
                // Pass the FULL objects to the UI State
                _uiState.value = AccountDetailsUiState.Success(
                    user = user,
                    profilePic = profilePic
                )
            } catch (e: Exception) {
                _uiState.value = AccountDetailsUiState.Error(
                    message = "Failed to load account details: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun updateGender(gender: Gender) {
        _uiState.update { currentState ->
            when (currentState) {
                is AccountDetailsUiState.Success -> currentState.copy(
                    user = currentState.user.copy(gender = gender)
                )
                else -> currentState
            }
        }
    }

    fun updateFirstName(firstName: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is AccountDetailsUiState.Success -> currentState.copy(
                    user = currentState.user.copy(firstName = firstName),
                    firstNameError = validateFirstName(firstName)
                )
                else -> currentState
            }
        }
    }

    fun updateLastName(lastName: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is AccountDetailsUiState.Success -> currentState.copy(
                    user = currentState.user.copy(lastName = lastName),
                    lastNameError = validateLastName(lastName)
                )
                else -> currentState
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is AccountDetailsUiState.Success -> currentState.copy(
                    user = currentState.user.copy(userEmail = email),
                    emailError = validateEmail(email)
                )
                else -> currentState
            }
        }
    }

    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is AccountDetailsUiState.Success -> currentState.copy(
                    user = currentState.user.copy(userPhoneNumber = phoneNumber),
                    phoneNumberError = validatePhoneNumber(phoneNumber)
                )
                else -> currentState
            }
        }
    }

    fun onProfilePictureChanged(uri: Uri) {
        viewModelScope.launch {
            // First, convert the image URI to a Base64 string.
            val newBase64 = convertUriToBase64(uri)

            // Now, update the UI state with this new Base64 string for the preview.
            _uiState.update { currentState ->
                if (currentState is AccountDetailsUiState.Success) {
                    currentState.copy(newImagePreview = newBase64)
                } else {
                    currentState
                }
            }
        }
    }

    fun saveAccountDetails() {
        val currentState = _uiState.value
        if (currentState !is AccountDetailsUiState.Success) return

        if (!currentState.isFormValid) return

        _uiState.update {
            if (it is AccountDetailsUiState.Success) {
                it.copy(isSaving = true)
            } else {
                it
            }
        }

        viewModelScope.launch {
            try {
                val userId = currentState.user.userID
                val updates = mapOf(
                    "gender" to currentState.user.gender,
                    "firstName" to currentState.user.firstName,
                    "lastName" to currentState.user.lastName,
                    "userEmail" to currentState.user.userEmail,
                    "userPhoneNumber" to currentState.user.userPhoneNumber
                )
                val detailsSuccess = userRepository.updateUser(userId, updates)
                if (!detailsSuccess) {
                    throw Exception("Failed to update user details.")
                }

                val pictureToSave = newProfilePicBase64
                if (pictureToSave != null) {
                    val pictureSuccess = profilePictureRepository.setProfilePicture(userId, pictureToSave)
                    if (!pictureSuccess) {
                        throw Exception("Failed to save profile picture.")
                    }
                }

                _saveSuccess.value = true

            } catch (e: Exception) {
                _uiState.value = AccountDetailsUiState.Error(
                    message = e.message ?: "Failed to save account details"
                )
            }
        }
    }

    fun onSaveHandled() {
        _saveSuccess.value = false
        _uiState.update {
            if (it is AccountDetailsUiState.Success) it.copy(isSaving = false) else it
        }
    }


    private fun validateAllFields(user: User): Boolean {
        val firstNameError = validateFirstName(user.firstName)
        val lastNameError = validateLastName(user.lastName)
        val emailError = validateEmail(user.userEmail)
        val phoneError = validatePhoneNumber(user.userPhoneNumber)

        _uiState.update { currentState ->
            when (currentState) {
                is AccountDetailsUiState.Success -> currentState.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                    emailError = emailError,
                    phoneNumberError = phoneError
                )
                else -> currentState
            }
        }

        return firstNameError == null && lastNameError == null &&
                emailError == null && phoneError == null
    }

    private fun validateFirstName(firstName: String): String? {
        return when {
            firstName.isBlank() -> "First name is required"
            firstName.length < 2 -> "First name must be at least 2 characters"
            firstName.length > 30 -> "First name must be less than 30 characters"
            !firstName.matches(Regex("^[a-zA-Z\\s]+$")) -> "First name can only contain letters"
            else -> null
        }
    }

    private fun validateLastName(lastName: String): String? {
        return when {
            lastName.isBlank() -> "Last name is required"
            lastName.length < 2 -> "Last name must be at least 2 characters"
            lastName.length > 30 -> "Last name must be less than 30 characters"
            !lastName.matches(Regex("^[a-zA-Z\\s]+$")) -> "Last name can only contain letters"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        val emailRegex = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        return when {
            email.isBlank() -> "Email is required"
            !emailRegex.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    private fun validatePhoneNumber(phoneNumber: String): String? {
        return when {
            phoneNumber.isNotBlank() && !phoneNumber.matches(Regex("^[0-9]{8,15}$")) -> "Invalid phone number format"
            else -> null
        }
    }


    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // Compress to reduce size
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}