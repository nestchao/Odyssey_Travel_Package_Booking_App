package com.example.mad_assignment.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType
import com.example.mad_assignment.data.repository.ProfilePicRepository
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
    private val profilePicRepository: ProfilePicRepository,
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

            if (user != null) {
                if (!user.isActive) {
                    auth.signOut()
                    _uiState.value = SignInUiState.Error(
                        message = "Your account has been deactivated. Please contact support."
                    )
                    return
                }
            }

            else {
                val nameParts = name?.trim()?.split(" ", limit = 2) ?: listOf()
                val firstName = nameParts.getOrNull(0) ?: ""
                val lastName = nameParts.getOrNull(1) ?: ""

                val newUser = User(
                    userID = userId,
                    firstName = firstName,
                    lastName = lastName,
                    userEmail = email ?: "no-email@example.com",
                    userType = UserType.CUSTOMER,
                    isActive = true
                )
                userRepository.createUser(newUser)

                profilePicRepository.setProfilePicture(
                    userId = userId,
                    base64Data = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAKVSURBVHhe7ZpdaxNBFIZFg14EAx48eBE8eBE8eBFE8eA/8Cf4CSx48eBFEERfPZUEi168iAY9iEaiadPc2QySSbZJdjYzb9Jkkn1n3p+dtDOd2UQiERERERERkYaqg2PWRQ3Yp2P05IAnV01Bf3eC/t5pSNYPeP78fGzYnU78+v0+L226T9d1/B4MBk/6+gHw+fyl3d/fw/v9Ppc1WcE2G4zJk48/T51OR/b5fP6y4yM/T5w9Y+a0M2bOWIZsChsYDIaYnJykv+dgMBhifHx8S+a0M2bOWIZsCiOTk5PsdDq+k+nk5CSbzaazZbYxY6kMjI6OdnVdJ2NifD5fsdvtZstsbLy/f0xMTk6yWq3msm02m2Xv3p2dna3JbC4vAGDXdXZ2trDZbLbb7aS93+/z+Xxubm4+NzDqun7a5/NJrVYbDocDAFxdXb1abU5PTwMAfPz8xMvLi6/X66urq2OxWJaamsIA8Pl89vj4mE6nk9FoNJfLHR4eAgA8PDyktrbW09MTiUSi1+t1Oh1B3mg0ms/nAYD5+XlWV1dZLBZvb2+pVCppa2sLAPj7+yubzVqtVq1Wy2q1urq6CgDg6ekpDRwOh2q1mvP5/OzsLAaDwdPTU97e3tJutwHA8fFxLik0NTUFAHB1dZWpqSnW6/X9/X0Oh4Oenh4Gg4FSqfT09BQAgI+Pj1Qqlaenpzw8PPB+v4vFotVqAQD4+vqKx+Px+Xw+n89qtXq8vARrt9utVqv1ejuOAEql0mq1ymw2W63W8fExvt/vfr9/cHCQi4sLptNpOp1+cHAAAFitVrlcLqPRaDQazWbz4eEh5eXlPR5PzWaz2Ww2m81mMxgMBgMAgImJCRaLRavVms1my+XyysoKptPp6enp09NTlmS2MWOpDImIiIiIiIiIiIiIiIiI1Ab/AfwSwbGEPYLxAAAAAElFTSuQmCC"
                )

                user = newUser
            }

            _uiState.value = SignInUiState.Success(user = user)

        } catch (e: Exception) {
            _uiState.value = SignInUiState.Error(
                message = "Failed to load user profile: ${e.message}"
            )
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