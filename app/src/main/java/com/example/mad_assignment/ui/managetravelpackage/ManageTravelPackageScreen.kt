package com.example.mad_assignment.ui.managetravelpackage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mad_assignment.data.model.ItineraryItem
import com.example.mad_assignment.data.model.PackageImage
import com.example.mad_assignment.data.model.Trip
import com.example.mad_assignment.util.toDataUri
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun manageTravelPackageScreen(
    navController: NavController,
    viewModel: ManageTravelPackageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris -> uris.forEach { viewModel.addImage(it) } }
    )

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Package" else "Add New Package") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { if (!uiState.isSaving) viewModel.savePackage() },
                    icon = {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                        }
                    },
                    text = { Text(if (uiState.isSaving) "Saving..." else "Save Package") }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item { FormSection("Basic Information") }
                item {
                    OutlinedTextField(
                        value = uiState.packageName,
                        onValueChange = viewModel::onPackageNameChange,
                        label = { Text("Package Name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.validationErrors.containsKey("packageName"),
                        supportingText = { uiState.validationErrors["packageName"]?.let { Text(it) } }
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.packageDescription,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        isError = uiState.validationErrors.containsKey("packageDescription"),
                        supportingText = { uiState.validationErrors["packageDescription"]?.let { Text(it) } },

                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = viewModel::onLocationChange,
                        label = { Text("Location (e.g., Kuala Lumpur)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.validationErrors.containsKey("location"),
                        supportingText = { uiState.validationErrors["location"]?.let { Text(it) } }
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.durationDays,
                        onValueChange = viewModel::onDurationChange,
                        label = { Text("Duration (Days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.validationErrors.containsKey("durationDays"),
                        supportingText = { uiState.validationErrors["durationDays"]?.let { Text(it) } }
                    )
                }

                item { FormSection("Pricing (RM)") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = uiState.pricing["Adult"] ?: "",
                            onValueChange = { viewModel.onPriceChange("Adult", it) },
                            label = { Text("Adult Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            isError = uiState.validationErrors.containsKey("price_adult"),
                            supportingText = { uiState.validationErrors["price_adult"]?.let { Text(it) } }
                        )
                        OutlinedTextField(
                            value = uiState.pricing["Child"] ?: "",
                            onValueChange = { viewModel.onPriceChange("Child", it) },
                            label = { Text("Child Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { FormSection("Images") }
                item {
                    ImagePickerSection(
                        images = uiState.displayedImages,
                        onAddClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemoveClick = viewModel::removeImage
                    )

                    uiState.validationErrors["images"]?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }


                item { FormSection("Itinerary") }
                item {
                    ItinerarySection(
                        uiState = uiState,
                        onSaveItem = viewModel::upsertItineraryItem,
                        onRemoveItem = viewModel::removeTripFromItinerary,
                        onEditItem = viewModel::onEditItineraryItem,
                        onAddNewItem = viewModel::onAddNewItineraryItem,
                        onDismissDialog = viewModel::onDismissItineraryDialog
                    )

                    uiState.validationErrors["itinerary"]?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                item { FormSection("Departure Dates & Capacity") }
                item {
                    uiState.validationErrors["departureDates"]?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                }
                items(uiState.packageOptions, key = { it.id }) { option ->
                    DepartureDateItem(
                        departure = option,
                        onDateChange = { date -> viewModel.onDepartureDateChange(option.id, date) },
                        onCapacityChange = { cap -> viewModel.onCapacityChange(option.id, cap) },
                        onRemove = { viewModel.removeDepartureDate(option.id) }
                    )
                }
                item {
                    Button(onClick = viewModel::addDepartureDate, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Departure Date")
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ItinerarySection(
    uiState: ManageTravelPackageUiState,
    onSaveItem: (item: ItineraryItem) -> Unit,
    onRemoveItem: (item: ItineraryItem) -> Unit,
    onEditItem: (item: ItineraryItem) -> Unit,
    onAddNewItem: (day: Int, tripId: String) -> Unit,
    onDismissDialog: () -> Unit
) {
    val duration = uiState.durationDays.toIntOrNull() ?: 0
    val tripMap = remember(uiState.availableTrips) {
        uiState.availableTrips.associateBy { it.tripId }
    }

    var dayToAddTo by rememberSaveable { mutableStateOf(0) }
    var showTripSelectionDialog by rememberSaveable { mutableStateOf(false) }

    // Dialog 1: For selecting a trip. This state is local as it's less critical.
    if (showTripSelectionDialog) {
        val tripsForDay = uiState.itineraries.filter { it.day == dayToAddTo }.map { it.tripId }.toSet()
        val availableTripsForDialog = uiState.availableTrips.filter { it.tripId !in tripsForDay }

        TripSelectionDialog(
            availableTrips = availableTripsForDialog,
            onDismiss = { showTripSelectionDialog = false },
            onTripSelected = { tripId ->
                showTripSelectionDialog = false
                // This now calls the ViewModel to open the details dialog
                onAddNewItem(dayToAddTo, tripId)
            }
        )
    }

    // Dialog 2: For editing details. Its visibility is now controlled by the ViewModel's state.
    uiState.editingItineraryItem?.let { itemToEdit ->
        ItineraryItemDetailsDialog(
            item = itemToEdit,
            tripName = tripMap[itemToEdit.tripId]?.tripName ?: "Unknown Trip",
            onDismiss = onDismissDialog,
            onSave = { updatedItem ->
                onSaveItem(updatedItem)
            }
        )
    }

    if (duration > 0) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            (1..duration).forEach { day ->
                ItineraryDayItem(
                    dayNumber = day,
                    itemsForDay = uiState.itineraries.filter { it.day == day },
                    tripMap = tripMap,
                    onAddClick = {
                        dayToAddTo = day
                        showTripSelectionDialog = true
                    },
                    onEditItem = onEditItem,
                    onRemoveItem = onRemoveItem
                )
            }
        }
    } else {
        Text(
            "Set a duration (in days) to start building the itinerary.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

@Composable
fun ItineraryDayItem(
    dayNumber: Int,
    itemsForDay: List<ItineraryItem>,
    tripMap: Map<String, Trip>,
    onAddClick: () -> Unit,
    onEditItem: (item: ItineraryItem) -> Unit,
    onRemoveItem: (item: ItineraryItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Day $dayNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            if (itemsForDay.isEmpty()) {
                Text(
                    "No activities planned for this day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Using ListItem to better display details
                itemsForDay.forEach { item ->
                    val trip = tripMap[item.tripId]
                    if (trip != null) {
                        ListItem(
                            headlineContent = { Text(trip.tripName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = {
                                Column {
                                    if (item.startTime.isNotBlank() || item.endTime.isNotBlank()) {
                                        Text("Time: ${item.startTime} - ${item.endTime}")
                                    }
                                    if (item.description.isNotBlank()) {
                                        Text(item.description)
                                    }
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { onRemoveItem(item) }) {
                                    Icon(Icons.Default.Close, "Remove Trip", tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEditItem(item) } // Make the whole item clickable
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Activity")
            }
        }
    }
}

@Composable
fun ItineraryItemDetailsDialog(
    item: ItineraryItem,
    tripName: String,
    onDismiss: () -> Unit,
    onSave: (ItineraryItem) -> Unit
) {
    var startTime by rememberSaveable { mutableStateOf(item.startTime) }
    var endTime by rememberSaveable { mutableStateOf(item.endTime) }
    var description by rememberSaveable { mutableStateOf(item.description) }

    var startTimeError by remember { mutableStateOf<String?>(null) }
    var endTimeError by remember { mutableStateOf<String?>(null) }

    fun validateTime(time: String): Boolean {
        if (time.isBlank()) return true
        return time.matches(Regex("^([01]\\d|2[0-3]):([0-5]\\d)$"))
    }

    fun validateFields(): Boolean {
        val isStartTimeValid = validateTime(startTime)
        startTimeError = if (isStartTimeValid) null else "Invalid format (HH:mm)"

        val isEndTimeValid = validateTime(endTime)
        endTimeError = if (isEndTimeValid) null else "Invalid format (HH:mm)"

        if (isStartTimeValid && isEndTimeValid && startTime.isNotBlank() && endTime.isNotBlank()) {
            if (endTime <= startTime) {
                endTimeError = "Must be after start time"
                return false
            }
        }

        return isStartTimeValid && isEndTimeValid
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Details for: $tripName", style = MaterialTheme.typography.titleLarge)
                Divider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {
                            startTime = it
                            startTimeError = null // Clear error on change
                        },
                        label = { Text("Start Time") },
                        placeholder = { Text("e.g., 09:00") },
                        modifier = Modifier.weight(1f),
                        isError = startTimeError != null,
                        supportingText = { startTimeError?.let { Text(it) } }
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {
                            endTime = it
                            endTimeError = null // Clear error on change
                        },
                        label = { Text("End Time") },
                        placeholder = { Text("e.g., 12:00") },
                        modifier = Modifier.weight(1f),
                        isError = endTimeError != null,
                        supportingText = { endTimeError?.let { Text(it) } }
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Meet at the main entrance") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (validateFields()) {
                            val updatedItem = item.copy(
                                startTime = startTime,
                                endTime = endTime,
                                description = description
                            )
                            onSave(updatedItem)
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ImagePickerSection(
    images: List<Any>,
    onAddClick: () -> Unit,
    onRemoveClick: (Any) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Image, contentDescription = "Add Image")
                    Text("Add Image", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(images) { image ->
            Box(modifier = Modifier.size(120.dp)) {
                AsyncImage(
                    model = when (image) {
                        is PackageImage -> toDataUri(image.base64Data)
                        is Uri -> image
                        else -> null
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { onRemoveClick(image) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove Image",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DepartureDateItem(
    departure: com.example.mad_assignment.data.model.DepartureAndEndTime,
    onDateChange: (Timestamp) -> Unit,
    onCapacityChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = departure.startDate.toDate().time,
        selectableDates = remember {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val today = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return utcTimeMillis >= today.timeInMillis
                }
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val utcDate = Date(it)
                        onDateChange(Timestamp(utcDate))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Departure Option", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove Option", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = departure.capacity.toString(),
                    onValueChange = onCapacityChange,
                    label = { Text("Capacity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text(formatter.format(departure.startDate.toDate()))
                }
            }
        }
    }
}

@Composable
fun TripSelectionDialog(
    availableTrips: List<Trip>,
    onDismiss: () -> Unit,
    onTripSelected: (tripId: String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Text(
                    "Select an Activity",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                Divider()
                if (availableTrips.isEmpty()) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No more activities to add.")
                    }
                } else {
                    LazyColumn {
                        items(availableTrips, key = { it.tripId }) { trip ->
                            ListItem(
                                headlineContent = { Text(trip.tripName) },
                                modifier = Modifier.clickable { onTripSelected(trip.tripId) }
                            )
                        }
                    }
                }
            }
        }
    }
}