package com.example.mad_assignment.ui.changepassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.ShowToastAndNavigateBack -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Change Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        when (val state = uiState) {
            is ChangePasswordUiState.Success -> {
                LaunchedEffect(state) {
                    // Show success message and navigate back
                    onNavigateBack()
                }
            }
            else -> {
                ChangePasswordContent(
                    uiState = state,
                    onUpdateOldPassword = viewModel::updateOldPassword,
                    onUpdateNewPassword = viewModel::updateNewPassword,
                    onUpdateConfirmPassword = viewModel::updateConfirmPassword,
                    onToggleOldPasswordVisibility = viewModel::toggleOldPasswordVisibility,
                    onToggleNewPasswordVisibility = viewModel::toggleNewPasswordVisibility,
                    onToggleConfirmPasswordVisibility = viewModel::toggleConfirmPasswordVisibility,
                    onSave = viewModel::changePassword
                )
            }
        }
    }
}

@Composable
private fun ChangePasswordContent(
    uiState: ChangePasswordUiState,
    onUpdateOldPassword: (String) -> Unit,
    onUpdateNewPassword: (String) -> Unit,
    onUpdateConfirmPassword: (String) -> Unit,
    onToggleOldPasswordVisibility: () -> Unit,
    onToggleNewPasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        val oldPassword = when (uiState) {
            is ChangePasswordUiState.Idle -> uiState.oldPassword
            is ChangePasswordUiState.Error -> uiState.oldPassword
            is ChangePasswordUiState.Loading -> uiState.oldPassword
            else -> ""
        }
        val newPassword = when (uiState) {
            is ChangePasswordUiState.Idle -> uiState.newPassword
            is ChangePasswordUiState.Error -> uiState.newPassword
            is ChangePasswordUiState.Loading -> uiState.newPassword
            else -> ""
        }
        val confirmPassword = when (uiState) {
            is ChangePasswordUiState.Idle -> uiState.confirmPassword
            is ChangePasswordUiState.Error -> uiState.confirmPassword
            is ChangePasswordUiState.Loading -> uiState.confirmPassword
            else -> ""
        }

        // Old Password
        PasswordTextField(
            label = "Old Password",
            value = oldPassword,
            onValueChange = onUpdateOldPassword,
            placeholder = "Old Password",
            isPasswordVisible = when (uiState) {
                is ChangePasswordUiState.Idle -> uiState.isOldPasswordVisible
                is ChangePasswordUiState.Error -> uiState.isOldPasswordVisible
                is ChangePasswordUiState.Loading -> uiState.isOldPasswordVisible
                else -> false
            },
            onTogglePasswordVisibility = onToggleOldPasswordVisibility,
            error = (uiState as? ChangePasswordUiState.Idle)?.oldPasswordError,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // New Password
        PasswordTextField(
            label = "New Password",
            value = newPassword,
            onValueChange = onUpdateNewPassword,
            placeholder = "New Password",
            isPasswordVisible = when (uiState) {
                is ChangePasswordUiState.Idle -> uiState.isNewPasswordVisible
                is ChangePasswordUiState.Error -> uiState.isNewPasswordVisible
                is ChangePasswordUiState.Loading -> uiState.isNewPasswordVisible
                else -> false
            },
            onTogglePasswordVisibility = onToggleNewPasswordVisibility,
            error = when (uiState) {
                is ChangePasswordUiState.Idle -> uiState.newPasswordError
                is ChangePasswordUiState.Error -> uiState.newPasswordError
                else -> null
            },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Confirm Password
        PasswordTextField(
            label = "Confirm Password",
            value = confirmPassword,
            onValueChange = onUpdateConfirmPassword,
            placeholder = "Confirm Password",
            isPasswordVisible = when (uiState) {
                is ChangePasswordUiState.Idle -> uiState.isConfirmPasswordVisible
                is ChangePasswordUiState.Error -> uiState.isConfirmPasswordVisible
                is ChangePasswordUiState.Loading -> uiState.isConfirmPasswordVisible
                else -> false
            },
            onTogglePasswordVisibility = onToggleConfirmPasswordVisibility,
            error = when (uiState) {
                is ChangePasswordUiState.Idle -> uiState.confirmPasswordError
                is ChangePasswordUiState.Error -> uiState.confirmPasswordError
                else -> null
            },
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Save Button
        Button(
            onClick = onSave,
            enabled = uiState is ChangePasswordUiState.Idle && uiState.isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (uiState is ChangePasswordUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.Black
                )
            } else {
                Text(
                    text = "Save",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PasswordTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.Gray
                )
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null
        )

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}