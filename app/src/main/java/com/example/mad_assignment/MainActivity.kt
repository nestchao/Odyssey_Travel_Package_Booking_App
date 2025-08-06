package com.example.mad_assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mad_assignment.Data.Package // <-- 1. IMPORT YOUR DATA CLASS
import com.example.mad_assignment.Screen.PackageCard
import com.example.mad_assignment.ui.theme.MAD_ASSIGNMENTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MAD_ASSIGNMENTTheme {
                // We wrap our card in a Box to center it on the screen for this example
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 2. CREATE A SAMPLE PACKAGE OBJECT
                    val samplePackage = Package(
                        packagesId = "p1",
                        packagesName = "Genting Highland",
                        review = 4.9,
                        // Use a real image URL for testing Coil
                        imageUrl = "https://media.tacdn.com/media/attractions-splice-spp-674x446/07/be/35/95.jpg"
                    )

                    // 3. CALL THE FUNCTION AND PASS THE DATA
                    PackageCard(
                        packageData = samplePackage,
                        modifier = Modifier.size(width = 250.dp, height = 300.dp)
                    )
                }
            }
        }
    }
}