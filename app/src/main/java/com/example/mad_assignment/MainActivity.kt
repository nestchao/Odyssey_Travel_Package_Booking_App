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
import androidx.core.view.WindowCompat
import com.example.mad_assignment.ui.AppNavigation
import com.example.mad_assignment.ui.theme.MAD_ASSIGNMENTTheme
import com.example.mad_assignment.util.DataUploader
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        setContent {
            MAD_ASSIGNMENTTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
//                    val scope = rememberCoroutineScope()
//                    val firestore = Firebase.firestore
//
//                    Column(
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .padding(top = 80.dp, end = 16.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Button(
//                            onClick = {
//                                scope.launch {
//                                    try {
//                                        Log.d("MainActivity", "Seed Database button clicked.")
//                                        DataUploader.seedDatabase(firestore)
//                                        Log.d("MainActivity", "Seeding process finished.")
//                                    } catch (e: Exception) {
//                                        Log.e("MainActivity", "Error during seeding", e)
//                                    }
//                                }
//                            }
//                        ) {
//                            Text("Seed Database")
//                        }
//                    }
                }
            }
        }
    }
}