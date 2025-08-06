package com.example.mad_assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mad_assignment.Data.Package
import com.example.mad_assignment.Screen.LoginPage
import com.example.mad_assignment.Screen.PackageCard
import com.example.mad_assignment.ui.theme.MAD_ASSIGNMENTTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MAD_ASSIGNMENTTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginPage(navController = navController)
                    }
                    composable("home") {
                        HomePage()
                    }
                }
            }
        }
    }
}

@Composable
fun HomePage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val samplePackage = Package(
            packagesId = "p1",
            packagesName = "Genting Highland",
            review = 4.9,
            imageUrl = "https://media.tacdn.com/media/attractions-splice-spp-674x446/07/be/35/95.jpg"
        )

        PackageCard(
            packageData = samplePackage,
            modifier = Modifier.size(width = 250.dp, height = 300.dp),
            navController = rememberNavController()
        )
    }
}