package com.example.mad_assignment.ui.manageuser

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.data.model.Gender
import com.example.mad_assignment.data.model.User
import com.example.mad_assignment.data.model.UserType

@Composable
fun ManageUserScreen(
    viewModel: ManageUserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Enhanced Header
        EnhancedAdminHeader(
            title = "Manage Users",
            subtitle = when (val state = uiState) {
                is ManageUserUiState.Success -> "${state.users.size} total users"
                else -> ""
            },
            onNavigateBack = onNavigateBack
        )

        when (val state = uiState) {
            is ManageUserUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading users...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            is ManageUserUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadUsers() }
                )
            }

            is ManageUserUiState.Success -> {
                ManageUserContent(
                    users = state.users,
                    selectedFilter = state.selectedFilter,
                    onFilterChanged = viewModel::filterUsers,
                    viewModel = viewModel,
                    showEditDialog = state.showEditDialog,
                    showDeleteDialog = state.showDeleteDialog,
                    selectedUser = state.selectedUser,
                    onEditUserClicked = viewModel::onEditUserClicked,
                    onDeleteUserClicked = viewModel::onDeleteUserClicked,
                    onDismissDialog = viewModel::onDismissDialog
                )
            }
        }
    }
}


@Composable
private fun EnhancedAdminHeader(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827)
        )

        Text(
            text = message,
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun ManageUserContent(
    users: List<User>,
    selectedFilter: UserType?,
    onFilterChanged: (UserType?) -> Unit,
    viewModel: ManageUserViewModel,
    showEditDialog: Boolean,
    showDeleteDialog: Boolean,
    selectedUser: User?,
    onEditUserClicked: (User) -> Unit,
    onDeleteUserClicked: (User) -> Unit,
    onDismissDialog: () -> Unit
) {

    val filteredUsers = if (selectedFilter != null) {
        users.filter { it.userType == selectedFilter }
    } else {
        users
    }

    val customerCount = users.count { it.userType == UserType.CUSTOMER }
    val adminCount = users.count { it.userType == UserType.ADMIN }

    Column {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Customers",
                        count = customerCount,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Admins",
                        count = adminCount,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                // Filter Section
                Column {
                    Text(
                        text = "Filter Users",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            EnhancedFilterChip(
                                label = "All Users",
                                count = users.size,
                                selected = selectedFilter == null,
                                onClick = { onFilterChanged(null) }
                            )
                        }
                        item {
                            EnhancedFilterChip(
                                label = "Customers",
                                count = customerCount,
                                selected = selectedFilter == UserType.CUSTOMER,
                                onClick = { onFilterChanged(UserType.CUSTOMER) }
                            )
                        }
                        item {
                            EnhancedFilterChip(
                                label = "Admins",
                                count = adminCount,
                                selected = selectedFilter == UserType.ADMIN,
                                onClick = { onFilterChanged(UserType.ADMIN) }
                            )
                        }
                    }
                }
            }

            if (filteredUsers.isEmpty()) {
                item {
                    EmptyState(
                        filter = selectedFilter,
                    )
                }
            } else {
                items(
                    items = filteredUsers,
                    key = { user -> user.userID }
                ) { user ->
                    EnhancedUserCard(
                        user = user,
                        onEdit = {
                            onEditUserClicked(user)
                        },
                        onDelete = {
                            onDeleteUserClicked(user)
                        }
                    )
                }
            }
        }
    }


    // Dialogs
    if (showEditDialog && selectedUser != null) {
        EnhancedEditUserInfoDialog(
            user = selectedUser,
            onDismiss = onDismissDialog,
            onConfirm = { updatedUser ->
                viewModel.updateUserInfo(updatedUser)
            }
        )
    }

    if (showDeleteDialog && selectedUser != null) {
        DeleteConfirmationDialog(
            user = selectedUser,
            onDismiss = onDismissDialog,
            onConfirm = {
                viewModel.deleteUser(selectedUser.userID)
            }
        )
    }
}


@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF374151)
            )
        }
    }
}

@Composable
private fun EnhancedFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) Color(0xFF6366F1) else Color.White
    val labelColor = if (selected) Color.White else Color(0xFF6B7280)
    val borderColor = if (selected) Color(0xFF6366F1) else Color(0xFFE5E7EB)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "$label ($count)",
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },

        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            selectedContainerColor = containerColor,
            selectedLabelColor = labelColor
        ),

        border = BorderStroke(1.dp, borderColor)
    )
}

@Composable
private fun EnhancedUserCard(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with gradient background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (user.userType == UserType.ADMIN)
                            Color(0xFFEF4444)
                        else
                            Color(0xFF6366F1)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${user.firstName.firstOrNull()?.uppercaseChar() ?: ""}${user.lastName.firstOrNull()?.uppercaseChar() ?: ""}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = user.userEmail,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Badge(
                    containerColor = if (user.userType == UserType.ADMIN)
                        Color(0xFFEF4444)
                    else
                        Color(0xFF10B981),
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = user.userType.name.lowercase().replaceFirstChar { it.titlecase() },
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit user",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete user",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    filter: UserType?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.PersonOff,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (filter != null) "No ${filter.name.lowercase()}s found" else "No users found",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151)
        )

        Text(
            text = if (filter != null) "Try changing your filter or add new users" else "Add some users to get started",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

    }
}


@Composable
private fun DeleteConfirmationDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = "Delete User",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete ${user.firstName} ${user.lastName}? This action cannot be undone.",
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun EnhancedEditUserInfoDialog(
    user: User,
    onDismiss: () -> Unit,
    onConfirm: (User) -> Unit
) {
    var firstName by remember { mutableStateOf(user.firstName) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var phoneNumber by remember { mutableStateOf(user.userPhoneNumber) }
    var gender by remember { mutableStateOf(user.gender) }
    var userType by remember { mutableStateOf(user.userType) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit User Information",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // First Name
                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        isError = it.isEmpty()
                    },
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError && firstName.isEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )

                // Last Name
                OutlinedTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        isError = it.isEmpty()
                    },
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isError && lastName.isEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )

                // Phone Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        focusedLabelColor = Color(0xFF6366F1)
                    )
                )

                // Gender Selection
                Column {
                    Text(
                        text = "Gender",
                        fontSize = 16.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.values().forEach { genderOption ->
                            FilterChip(
                                selected = gender == genderOption,
                                onClick = { gender = genderOption },
                                label = {
                                    Text(
                                        text = genderOption.name,
                                        fontSize = 14.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // User Type Selection
                Column {
                    Text(
                        text = "User Type",
                        fontSize = 16.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserType.values().forEach { type ->
                            FilterChip(
                                selected = userType == type,
                                onClick = { userType = type },
                                label = {
                                    Text(
                                        text = type.name,
                                        fontSize = 14.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF6366F1),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        onConfirm(
                            user.copy(
                                firstName = firstName,
                                lastName = lastName,
                                userPhoneNumber = phoneNumber,
                                gender = gender,
                                userType = userType
                            )
                        )
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF6B7280)
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}