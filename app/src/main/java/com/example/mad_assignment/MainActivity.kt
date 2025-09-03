package com.example.mad_assignment

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mad_assignment.ui.AppNavigation
import com.example.mad_assignment.ui.theme.MAD_ASSIGNMENTTheme
import com.example.mad_assignment.util.FirebaseSeeder
import com.google.firebase.firestore.ktx.BuildConfig
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MAD_ASSIGNMENTTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // This is your main app UI
                    AppNavigation()

                    // --- This is your temporary "admin" section ---
//                    val scope = rememberCoroutineScope()
//                    val firestore = Firebase.firestore

//                    Column(
//                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
////                         Button to upload a full package
//                        Button(
//                            onClick = {
//                                scope.launch {
//                                    DataUploader
//                                }
//                            }
//                        ) {
//                            Text("Upload Sample Package")
//                        }

//                         --- ADD THIS NEW BUTTON ---
//                        Button(
//                            onClick = {
//                                scope.launch {
//                                    val newTripId = uploadNewTrip(firestore)
//                                    if (newTripId != null) {
//                                        Log.d("MainActivity", "New trip created with ID: $newTripId")
//                                        // You could show a Toast message here for confirmation
//                                    }
//                                }
//                            },
//                            modifier = Modifier.padding(top = 8.dp)
//                        ) {
//                            Text("Upload New Trip")
//                        }
                    }
                }
            }
        }
    }

