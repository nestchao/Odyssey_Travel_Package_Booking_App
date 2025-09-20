package com.example.mad_assignment.ui.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mad_assignment.R
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.ui.signin.SignInUiState

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = viewModel(),
    onSignUpSuccess: (User) -> Unit = {},
    onNavigateToSignIn: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // React to sign-up success
    LaunchedEffect(uiState) {
        val success = uiState as? SignUpUiState.Success
        if (success != null) {
            onSignUpSuccess(success.user)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4A90E2),
                        Color(0xFF7B68EE),
                        Color(0xFF9D50BB)
                    )
                )
            )
    ) {
        when (uiState) {
            is SignUpUiState.Loading -> {
                LoadingScreen()
            }
            else -> {
                SignUpContent(
                    uiState = uiState,
                    viewModel = viewModel,
                    focusManager = focusManager,
                    onNavigateToSignIn = onNavigateToSignIn
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(160.dp)
                .shadow(20.dp, CircleShape),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.96f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF4A90E2),
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Creating account...",
                        fontSize = 12.sp,
                        color = Color(0xFF4A90E2),
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SignUpContent(
    uiState: SignUpUiState,
    viewModel: SignUpViewModel,
    focusManager: androidx.compose.ui.focus.FocusManager,
    onNavigateToSignIn: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // App Logo
        Card(
            modifier = Modifier
                .size(100.dp)
                .shadow(12.dp, CircleShape),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.odyssey_logo),
                    contentDescription = null,
                    modifier = Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Welcome Text
        Text(
            text = "Create Account",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Join us and start your adventure",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Sign Up Form Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp)
            ) {
                // Name Field
                ModernTextField(
                    value = when (uiState) {
                        is SignUpUiState.Idle -> uiState.name
                        is SignUpUiState.Error -> uiState.name
                        else -> ""
                    },
                    onValueChange = { viewModel.onNameChange(it) },
                    label = "Full Name",
                    icon = Icons.Default.Person,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    isError = (uiState as? SignUpUiState.Error)?.nameError != null,
                    errorMessage = (uiState as? SignUpUiState.Error)?.nameError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                ModernTextField(
                    value = when (uiState) {
                        is SignUpUiState.Idle -> uiState.email
                        is SignUpUiState.Error -> uiState.email
                        else -> ""
                    },
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = "Email Address",
                    icon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    isError = (uiState as? SignUpUiState.Error)?.emailError != null,
                    errorMessage = (uiState as? SignUpUiState.Error)?.emailError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Number Field
                ModernTextField(
                    value = when (uiState) {
                        is SignUpUiState.Idle -> uiState.phoneNumber
                        is SignUpUiState.Error -> uiState.phoneNumber
                        else -> ""
                    },
                    onValueChange = { viewModel.onPhoneNumberChange(it) },
                    label = "Phone Number",
                    icon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    isError = (uiState as? SignUpUiState.Error)?.phoneNumberError != null,
                    errorMessage = (uiState as? SignUpUiState.Error)?.phoneNumberError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                val isPasswordVisible = when (uiState) {
                    is SignUpUiState.Idle -> uiState.isPasswordVisible
                    is SignUpUiState.Error -> uiState.isPasswordVisible
                    else -> false
                }

                ModernPasswordField(
                    value = when (uiState) {
                        is SignUpUiState.Idle -> uiState.password
                        is SignUpUiState.Error -> uiState.password
                        else -> ""
                    },
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = "Password",
                    isPasswordVisible = isPasswordVisible,
                    onTogglePasswordVisibility = { viewModel.onTogglePasswordVisibility() },
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    isError = (uiState as? SignUpUiState.Error)?.passwordError != null,
                    errorMessage = (uiState as? SignUpUiState.Error)?.passwordError
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                val isConfirmPasswordVisible = when (uiState) {
                    is SignUpUiState.Idle -> uiState.isConfirmPasswordVisible
                    is SignUpUiState.Error -> uiState.isConfirmPasswordVisible
                    else -> false
                }

                ModernPasswordField(
                    value = when (uiState) {
                        is SignUpUiState.Idle -> uiState.confirmPassword
                        is SignUpUiState.Error -> uiState.confirmPassword
                        else -> ""
                    },
                    onValueChange = { viewModel.onConfirmPasswordChange(it) },
                    label = "Confirm Password",
                    isPasswordVisible = isConfirmPasswordVisible,
                    onTogglePasswordVisibility = { viewModel.onToggleConfirmPasswordVisibility() },
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        focusManager.clearFocus()
                        viewModel.signUp()
                    },
                    isError = (uiState as? SignUpUiState.Error)?.confirmPasswordError != null,
                    errorMessage = (uiState as? SignUpUiState.Error)?.confirmPasswordError
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sign Up Button

                Button(
                    onClick = { viewModel.signUp() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF667eea)
                    ),
                    enabled = uiState !is SignUpUiState.Loading
                ) {
                    Text(
                        text = "Sign In",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Error Message
                (uiState as? SignUpUiState.Error)?.message?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Sign In Link
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp
                )
                Text(
                    text = "Sign In",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignIn() }
                )
            }

        Spacer(modifier = Modifier.height(60.dp))
        }
}



@Composable
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4A90E2)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4A90E2),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFF4A90E2),
                unfocusedLabelColor = Color.Gray.copy(alpha = 0.7f)
            )
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ModernPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else Color(0xFF4A90E2)
                )
            },
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color.Gray
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4A90E2),
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedLabelColor = Color(0xFF4A90E2),
                unfocusedLabelColor = Color.Gray.copy(alpha = 0.7f)
            )
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
