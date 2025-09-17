package com.example.mad_assignment.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mad_assignment.ui.home.HomeScreen
import com.example.mad_assignment.ui.notifications.NotificationDetailsScreen
import com.example.mad_assignment.ui.notifications.NotificationsScreen
import com.example.mad_assignment.ui.packagedetail.PackageDetailScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home"){
        composable("home"){
            HomeScreen(
                onPackageClick = { packageId ->
                    navController.navigate("detail/$packageId")
                },
                onBellClick = {
                    navController.navigate("notifications")
                }
            )
        }
        composable(
            route = "detail/{packageId}",
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ){
            PackageDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("notifications") {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNotificationClick = { notificationId ->
                    navController.navigate("notificationDetail/$notificationId")
                }
            )
        }
        composable("notificationDetail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            NotificationDetailsScreen(
                notificationId = id,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}