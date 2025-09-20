package com.example.mad_assignment.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mad_assignment.ui.forgetpassword.ForgotPasswordScreen
import com.example.mad_assignment.ui.forgetpassword.ForgotPasswordViewModel
import com.example.mad_assignment.ui.home.HomeScreen
import com.example.mad_assignment.ui.packagedetail.PackageDetailScreen
import com.example.mad_assignment.ui.profile.ProfileScreen
import com.example.mad_assignment.ui.profile.ProfileViewModel
import com.example.mad_assignment.ui.signin.SignInScreen
import com.example.mad_assignment.ui.signin.SignInViewModel
import com.example.mad_assignment.ui.signup.SignUpScreen
import com.example.mad_assignment.ui.signup.SignUpViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "signin" // Start with sign in screen
    ) {
        // Sign In Screen
        composable("signin") {
            val viewModel: SignInViewModel = hiltViewModel()
            SignInScreen(
                viewModel = viewModel,
                onSignInSuccess = { user ->
                    // Navigate to main app after successful sign in
                    navController.navigate("home") {
                        // Clear the back stack so user can't go back to signin
                        popUpTo("signin") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    // Navigate to sign up screen
                    navController.navigate("signup")
                },
                onForgotPassword = {
                    // Navigate to forgot password screen
                    navController.navigate("forgot_password")
                }
            )
        }

        // Sign Up
        composable("signup") {
            val viewModel: SignUpViewModel = hiltViewModel()
            SignUpScreen(
                viewModel = viewModel,
                onSignUpSuccess = { user ->
                    // Navigate to main app after successful sign up
                    navController.navigate("home") {
                        // Clear the back stack so user can't go back to signup
                        popUpTo("signin") { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    // Go back to sign in screen
                    navController.navigate("signin")
                }
            )
        }

        // Forgot Password
        composable("forgot_password") {
            val viewModel: ForgotPasswordViewModel = hiltViewModel()
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onResetSuccess = {
                    navController.popBackStack()
                }
            )
        }

        // Home (with userId passed from sign-in)
        composable(
            route = "home",

        ) {
            HomeScreen(

                onPackageClick = { packageId ->
                    navController.navigate("detail/$packageId")
                }
            )
        }

        // Package Details
        composable(
            route = "detail/{packageId}",
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ) {
            PackageDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToBooking = { navController.navigate("booking") },
            )
        }


    }
}
