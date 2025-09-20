package com.example.mad_assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.mad_assignment.ui.AppNavigation
import com.example.mad_assignment.ui.theme.MAD_ASSIGNMENTTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MAD_ASSIGNMENTTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // This is your main app UI
                    AppNavigation()


                    }
                }
            }
        }
    }

