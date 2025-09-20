package com.example.mad_assignment.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import com.example.mad_assignment.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
class SignInViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.Idle())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        val firebaseUser = auth.currentUser
        if (firebaseUser != null) {
            _uiState.value = SignInUiState.Loading()
            viewModelScope.launch {
                fetchOrCreateUserData(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email)
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignInUiState.Idle -> currentState.copy(email = email, emailError = null)
                is SignInUiState.Error -> currentState.copy(email = email, emailError = null)
                else -> currentState
            }
        }
    }

    fun updatePassword(password: String) {
        _uiState.update { currentState ->
            when (currentState) {
                is SignInUiState.Idle -> currentState.copy(password = password, passwordError = null)
                is SignInUiState.Error -> currentState.copy(password = password, passwordError = null)
                else -> currentState
            }
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { currentState ->
            when (currentState) {
                is SignInUiState.Idle -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                is SignInUiState.Error -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                is SignInUiState.Loading -> currentState.copy(isPasswordVisible = !currentState.isPasswordVisible)
                else -> currentState
            }
        }
    }


    fun signIn() {
        val currentState = _uiState.value
        val email = (currentState as? SignInUiState.Idle)?.email ?: (currentState as? SignInUiState.Error)?.email ?: return
        val password = (currentState as? SignInUiState.Idle)?.password ?: (currentState as? SignInUiState.Error)?.password ?: return

        // Validation
        if (!validateInputs(email, password)) return

        _uiState.value = SignInUiState.Loading(email = email, password = password)

        viewModelScope.launch {
            try {
                val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // Use the more robust fetch or create method
                    fetchOrCreateUserData(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email)
                } else {
                    throw Exception("Authentication failed")
                }
            } catch (e: Exception) {
                _uiState.value = SignInUiState.Error(
                    message = getAuthErrorMessage(e),
                    email = email,
                    password = password
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = SignInUiState.Loading()
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    // This is crucial for Google Sign-In: check if the user is new to our database
                    fetchOrCreateUserData(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email)
                } else {
                    throw Exception("Google Sign-In failed.")
                }
            } catch (e: Exception) {
                _uiState.value = SignInUiState.Error(
                    message = e.message ?: "An unknown Google Sign-In error occurred."
                )
            }
        }
    }

    private suspend fun fetchOrCreateUserData(userId: String, name: String?, email: String?) {
        try {
            var user = userRepository.getUserById(userId)

            // If user is null, they exist in Auth but not in our database. Create them.
            if (user == null) {
                val newUser = User(
                    userID = userId,
                    userName = name ?: "No Name",
                    userEmail = email ?: "no-email@example.com",
                    userType = UserType.CUSTOMER
                )
                userRepository.createUser(newUser)
                user = newUser
            }
            _uiState.value = SignInUiState.Success(user = user)

        } catch (e: Exception) {
            _uiState.value = SignInUiState.Error(
                message = "Failed to load user profile: ${e.message}"
            )
        }
    }

    fun signOut() {
        auth.signOut()
        _uiState.value = SignInUiState.Idle()
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            if (validateEmail(email) == null) {
                try {
                    auth.sendPasswordResetEmail(email).await()
                    // You could emit a one-time event here to show a success message
                } catch (e: Exception) {
                    // Emit an event to show an error message
                }
            }
        }
    }

    // --- Helper Functions ---

    private fun validateInputs(email: String, pass: String): Boolean {
        val emailError = validateEmail(email)
        val passwordError = validatePassword(pass)

        if (emailError != null || passwordError != null) {
            _uiState.update { currentState ->
                val vis = (currentState as? SignInUiState.Idle)?.isPasswordVisible ?: (currentState as? SignInUiState.Error)?.isPasswordVisible ?: false
                SignInUiState.Error(
                    message = "Please fix the errors",
                    email = email,
                    password = pass,
                    isPasswordVisible = vis,
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return false
        }
        return true
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

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    private fun getAuthErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The email address is badly formatted." -> "Invalid email format"
            "The password is invalid or the user does not have a password." -> "Invalid password or email"
            "There is no user record corresponding to this identifier. The user may have been deleted." -> "No account found with this email"
            "The user account has been disabled by an administrator." -> "Your account has been disabled"
            "An internal error has occurred. [ 7: ]" -> "Invalid password or email" // Common Firebase error for wrong password
            else -> "Sign in failed: ${exception.message ?: "Unknown error"}"
        }
    }
}