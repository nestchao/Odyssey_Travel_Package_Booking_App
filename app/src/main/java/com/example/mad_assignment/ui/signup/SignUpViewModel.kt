package com.example.mad_assignment.ui.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import com.example.mad_assignment.data.repository.ProfilePicRepository
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val profilePicRepository: ProfilePicRepository,
    private val auth: FirebaseAuth
) : ViewModel() { // Removed the empty parentheses

    private val _uiState = MutableStateFlow<SignUpUiState>(SignUpUiState.Idle())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()



    fun onNameChange(name: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(name = name, nameError = null)
                is SignUpUiState.Error -> currentState.copy(name = name, nameError = null)
                else -> currentState
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(email = email, emailError = null)
                is SignUpUiState.Error -> currentState.copy(email = email, emailError = null)
                else -> currentState
            }
        }
    }

    fun onPhoneNumberChange(phoneNumber: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(phoneNumber = phoneNumber, phoneNumberError = null)
                is SignUpUiState.Error -> currentState.copy(phoneNumber = phoneNumber, phoneNumberError = null)
                else -> currentState
            }
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(password = password, passwordError = null)
                is SignUpUiState.Error -> currentState.copy(password = password, passwordError = null)
                else -> currentState
            }
        }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(confirmPassword = confirmPassword, confirmPasswordError = null)
                is SignUpUiState.Error -> currentState.copy(confirmPassword = confirmPassword, confirmPasswordError = null)
                else -> currentState
            }
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                is SignUpUiState.Error -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                else -> currentState
            }
        }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { currentState ->
            when (currentState) {
                is SignUpUiState.Idle -> currentState.copy(isConfirmPasswordVisible = !currentState.isConfirmPasswordVisible)
                is SignUpUiState.Error -> currentState.copy(isConfirmPasswordVisible = !currentState.isConfirmPasswordVisible)
                else -> currentState
            }
        }
    }

    fun signUp() {
        val currentState = _uiState.value
        if (currentState !is SignUpUiState.Idle && currentState !is SignUpUiState.Error) return

        val name = (currentState as? SignUpUiState.Idle)?.name ?: (currentState as? SignUpUiState.Error)?.name ?: ""
        val email = (currentState as? SignUpUiState.Idle)?.email ?: (currentState as? SignUpUiState.Error)?.email ?: ""
        val phoneNumber = (currentState as? SignUpUiState.Idle)?.phoneNumber ?: (currentState as? SignUpUiState.Error)?.phoneNumber ?: ""
        val password = (currentState as? SignUpUiState.Idle)?.password ?: (currentState as? SignUpUiState.Error)?.password ?: ""
        val confirmPassword = (currentState as? SignUpUiState.Idle)?.confirmPassword ?: (currentState as? SignUpUiState.Error)?.confirmPassword ?: ""

        if (!validateInputs(name, email, phoneNumber, password, confirmPassword)) {
            return
        }

        _uiState.value = SignUpUiState.Loading(name, email, phoneNumber)

        viewModelScope.launch {
            try {

                val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val firebaseUser = authResult.user ?: throw Exception("Firebase user not found after creation.")

                val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
                firebaseUser.updateProfile(profileUpdates).await()

                val newUser = User(
                    userID = firebaseUser.uid,
                    userName = name,
                    userEmail = email.trim(),
                    userPhoneNumber = phoneNumber,
                    userType = UserType.CUSTOMER
                )

                userRepository.createUser(newUser)

                profilePicRepository.setProfilePicture(
                    userId = firebaseUser.uid,
                    base64Data = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAKVSURBVHhe7ZpdaxNBFIZFg14EAx48eBE8eBE8eBFE8eA/8Cf4CSx48eBFEERfPZUEi168iAY9iEaiadPc2QySSbZJdjYzb9Jkkn1n3p+dtDOd2UQiERERERERkYaqg2PWRQ3Yp2P05IAnV01Bf3eC/t5pSNYPeP78fGzYnU78+v0+L226T9d1/B4MBk/6+gHw+fyl3d/fw/v9Ppc1WcE2G4zJk48/T51OR/b5fP6y4yM/T5w9Y+a0M2bOWIZsChsYDIaYnJykv+dgMBhifHx8S+a0M2bOWIZsCiOTk5PsdDq+k+nk5CSbzaazZbYxY6kMjI6OdnVdJ2NifD5fsdvtZstsbLy/f0xMTk6yWq3msm02m2Xv3p2dna3JbC4vAGDXdXZ2trDZbLbb7aS93+/z+Xxubm4+NzDqun7a5/NJrVYbDocDAFxdXb1abU5PTwMAfPz8xMvLi6/X66urq2OxWJaamsIA8Pl89vj4mE6nk9FoNJfLHR4eAgA8PDyktrbW09MTiUSi1+t1Oh1B3mg0ms/nAYD5+XlWV1dZLBZvb2+pVCppa2sLAPj7+yubzVqtVq1Wy2q1urq6CgDg6ekpDRwOh2q1mvP5/OzsLAaDwdPTU97e3tJutwHA8fFxLik0NTUFAHB1dZWpqSnW6/X9/X0Oh4Oenh4Gg4FSqfT09BQAgI+Pj1Qqlaenpzw8PPB+v4vFotVqAQD4+vqKx+Px+Xw+n89qtXq8vARrt9utVqv1ejuOAEql0mq1ymw2W63W8fExvt/vfr9/cHCQi4sLptNpOp1+cHAAAFitVrlcLqPRaDQazWbz4eEh5eXlPR5PzWaz2Ww2m81mMxgMBgMAgImJCRaLRavVms1my+XyysoKptPp6enp09NTlmS2MWOpDImIiIiIiIiIiIiIiIiI1Ab/AfwSwbGEPYLxAAAAAElFTSuQmCC"
                )

                _uiState.value = SignUpUiState.Success(user = newUser)

            } catch (e: Exception) {
                _uiState.value = SignUpUiState.Error(
                    message = getAuthErrorMessage(e),
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber,
                    password = password,
                    confirmPassword = confirmPassword
                )
            }
        }
    }

    // --- Private Helper Functions ---

    private fun validateInputs(name: String, email: String, phone: String, pass: String, confirmPass: String): Boolean {
        val nameError = if (name.isBlank()) "Name is required" else null
        val emailError = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
        val phoneNumberError = if (phone.isBlank()) "Phone number is required" else null
        val passwordError = when {
            pass.isBlank() -> "Password is required"
            pass.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        val confirmPasswordError = when {
            confirmPass.isBlank() -> "Please confirm your password"
            pass != confirmPass -> "Passwords do not match"
            else -> null
        }

        val hasError = listOfNotNull(nameError, emailError, phoneNumberError, passwordError, confirmPasswordError).isNotEmpty()

        if (hasError) {
            _uiState.value = SignUpUiState.Error(
                message = "Please fix the errors below",
                name = name,
                email = email,
                phoneNumber = phone,
                password = pass,
                confirmPassword = confirmPass,
                nameError = nameError,
                emailError = emailError,
                phoneNumberError = phoneNumberError,
                passwordError = passwordError,
                confirmPasswordError = confirmPasswordError
            )
            return false
        }
        return true
    }

    // ADDED: This function was missing
    private fun getAuthErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The email address is already in use by another account." -> "This email is already registered."
            "The email address is badly formatted." -> "Please enter a valid email address."
            else -> "Sign up failed: ${exception.message ?: "An unknown error occurred."}"
        }
    }
}