package com.example.mad_assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mad_assignment.ui.AppNavigation
import com.example.mad_assignment.ui.home.HomeScreen
import com.example.mad_assignment.ui.theme.MAD_ASSIGNMENTTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MAD_ASSIGNMENTTheme {
                AppNavigation()
            }
        }
    }
}

