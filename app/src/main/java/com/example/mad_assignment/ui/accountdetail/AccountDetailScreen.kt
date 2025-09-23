package com.example.mad_assignment.ui.accountdetail

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.data.model.Gender
import com.example.mad_assignment.ui.profile.base64ToImageBitmap

@Composable
fun AccountDetailsScreen(
    viewModel: AccountDetailsViewModel = hiltViewModel(),
    onNavigateBack: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val isSaveSuccessful by viewModel.saveSuccess.collectAsStateWithLifecycle()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onProfilePictureChanged(uri)
            }
        }
    )

    LaunchedEffect(isSaveSuccessful) {
        if (isSaveSuccessful) {
            onNavigateBack(true)
            viewModel.onSaveHandled()
        }
    }

    BackHandler {
        onNavigateBack(false)
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
                onClick = { onNavigateBack(false) },
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
                text = "Account Details",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        when (val state = uiState) {
            is AccountDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AccountDetailsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(onClick = { viewModel.loadAccountDetails() }) {
                        Text("Retry")
                    }
                }
            }

            is AccountDetailsUiState.Success -> {
                AccountDetailsContent(
                    state = state,
                    onEditPictureClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    onUpdateGender = viewModel::updateGender,
                    onUpdateFirstName = viewModel::updateFirstName,
                    onUpdateLastName = viewModel::updateLastName,
                    onUpdateEmail = viewModel::updateEmail,
                    onUpdatePhoneNumber = viewModel::updatePhoneNumber,
                    onSave = viewModel::saveAccountDetails,
                    modifier = Modifier.verticalScroll(scrollState)
                )
            }
        }
    }
}

@Composable
private fun AccountDetailsContent(
    state: AccountDetailsUiState.Success,
    onEditPictureClick: () -> Unit,
    onUpdateGender: (Gender) -> Unit,
    onUpdateFirstName: (String) -> Unit,
    onUpdateLastName: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdatePhoneNumber: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val base64ToShow = state.newImagePreview ?: state.profilePic.profilePictureBase64
    val imageBitmap = remember(base64ToShow) {
        base64ToImageBitmap(base64ToShow)
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Profile Picture Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(100.dp)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(2.dp, Color.Gray, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile Icon",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F5F5))
                            .border(2.dp, Color.Gray, CircleShape),
                    )
                }

                // Edit Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .align(Alignment.BottomEnd)
                        .clickable { onEditPictureClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Gender Section
        Text(
            text = "Gender",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenderOption(
                text = "Sir",
                selected = state.user.gender == Gender.SIR,
                onSelect = { onUpdateGender(Gender.SIR) }
            )
            GenderOption(
                text = "Mrs.",
                selected = state.user.gender == Gender.MRS,
                onSelect = { onUpdateGender(Gender.MRS) }
            )
        }

        // Name Section
        Text(
            text = "Name",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AccountTextField(
            label = "First Name",
            value = state.user.firstName,
            onValueChange = onUpdateFirstName,
            placeholder = "Your first name",
            error = state.firstNameError,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AccountTextField(
            label = "Last Name",
            value = state.user.lastName,
            onValueChange = onUpdateLastName,
            placeholder = "Your last name",
            error = state.lastNameError,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Contact Information Section
        Text(
            text = "Email & Phone Number",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        AccountTextField(
            label = "Email",
            value = state.user.userEmail,
            onValueChange = onUpdateEmail,
            placeholder = "youremail@gmail.com",
            error = state.emailError,
            keyboardType = KeyboardType.Email,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        PhoneNumberField(
            label = "Phone Number",
            phoneNumber = state.user.userPhoneNumber,
            onPhoneNumberChange = onUpdatePhoneNumber,
            error = state.phoneNumberError,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Save Button
        Button(
            onClick = onSave,
            enabled = state.isFormValid && !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (state.isSaving) {
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
private fun GenderOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = onSelect
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color.Black,
                unselectedColor = Color.Gray
            )
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun AccountTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
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
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

@Composable
private fun PhoneNumberField(
    label: String,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    error: String?,
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
            value = phoneNumber,
            onValueChange = onPhoneNumberChange,
            placeholder = { Text("01234567890", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Gray,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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