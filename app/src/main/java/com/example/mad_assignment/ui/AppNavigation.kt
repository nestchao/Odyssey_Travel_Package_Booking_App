package com.example.mad_assignment.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mad_assignment.ui.home.HomeScreen
import com.example.mad_assignment.ui.packagedetail.PackageDetailScreen
import com.example.mad_assignment.ui.cart.CartScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPackageClick = { packageId ->
                    navController.navigate("detail/$packageId")
                },
                onNavigateToCart = {
                    navController.navigate("cart")
                }
            )
        }
        composable(
            route = "detail/{packageId}",
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ){
            PackageDetailScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable("cart") {
            CartScreen(
                onBackClick = { navController.popBackStack() },
                onPackageDetailsClick = { cartItem ->
                    navController.navigate("detail/${cartItem.packageId}")
                },
                onPackagesClick = { /* TODO: navigate to available packages list screen or home screen */ },
                onCheckoutClick = { cartItems ->
                    /* TODO: navigate to payment: pass list of cart items to process payment */
                }
            )
        }
    }
}
