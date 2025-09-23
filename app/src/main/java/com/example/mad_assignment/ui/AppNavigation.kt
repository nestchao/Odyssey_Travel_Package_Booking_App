package com.example.mad_assignment.ui


import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mad_assignment.data.model.UserType
import com.example.mad_assignment.ui.aboutus.AboutUsScreen
import com.example.mad_assignment.ui.aboutus.AboutUsViewModel
import com.example.mad_assignment.ui.accountdetail.AccountDetailsScreen
import com.example.mad_assignment.ui.accountdetail.AccountDetailsViewModel
import com.example.mad_assignment.ui.admindashboard.AdminDashboardScreen
import com.example.mad_assignment.ui.admindashboard.AdminDashboardViewModel
import com.example.mad_assignment.ui.changepassword.ChangePasswordScreen
import com.example.mad_assignment.ui.changepassword.ChangePasswordViewModel
import com.example.mad_assignment.ui.explore.ExploreScreen
import com.example.mad_assignment.ui.forgetpassword.ForgotPasswordScreen
import com.example.mad_assignment.ui.forgetpassword.ForgotPasswordViewModel
import com.example.mad_assignment.ui.home.EnhancedBottomNavigationBar
import com.example.mad_assignment.ui.home.HomeScreen
import com.example.mad_assignment.ui.notifications.NotificationDetailsScreen
import com.example.mad_assignment.ui.notifications.NotificationSchedulerScreen
import com.example.mad_assignment.ui.notifications.NotificationsScreen
import com.example.mad_assignment.ui.packagedetail.PackageDetailScreen
import com.example.mad_assignment.ui.profile.ProfileScreen
import com.example.mad_assignment.ui.profile.ProfileViewModel
import com.example.mad_assignment.ui.recentlyviewed.RecentlyViewedScreen
import com.example.mad_assignment.ui.search.SearchScreen
import com.example.mad_assignment.ui.settings.SettingsScreen
import com.example.mad_assignment.ui.settings.SettingsViewModel
import com.example.mad_assignment.ui.signin.SignInScreen
import com.example.mad_assignment.ui.signin.SignInViewModel
import com.example.mad_assignment.ui.signup.SignUpScreen
import com.example.mad_assignment.ui.signup.SignUpViewModel
import com.example.mad_assignment.ui.wishlist.WishlistScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    val mainDestination = if (isTablet) "tablet_main" else "phone_main"

    NavHost(navController = navController, startDestination = "signin") {

        composable("signin") {
            val viewModel: SignInViewModel = hiltViewModel()
            SignInScreen(
                viewModel = viewModel,
                onSignInSuccess = { user ->
                    val destination = when (user.userType) {
                        UserType.ADMIN -> "admin_dashboard"
                        UserType.CUSTOMER -> mainDestination
                    }
                    navController.navigate(destination) {
                        popUpTo("signin") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onForgotPassword = {
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
                    navController.navigate(mainDestination) {
                        popUpTo("signin") { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
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
                    navController.navigate("signin")
                },
                onResetSuccess = {
                    navController.navigate("signin")
                }
            )
        }


        composable("phone_main") { navBackStackEntry ->

            PhoneContainerScreen(
                onNavigateToDetail = { packageId -> navController.navigate("detail/$packageId") },
                onNavigateToSearch = { navController.navigate("search") },
                onBellClick = { navController.navigate("notifications") },
                onSignOut = { navController.navigate("signin") { popUpTo(navController.graph.startDestinationId) { inclusive = true } } },
                onNavigateToAccountDetails = { navController.navigate("account_detail") },
                onNavigateToSettings = { navController.navigate("setting") },
                onNavigateToRecentlyViewed = { navController.navigate("recentlyViewed") },
                onNavigateToWishlist = { navController.navigate("wishlist") },
            )
        }

        composable("tablet_main") { navBackStackEntry ->
            TabletContainerScreen(
                onNavigateToDetail = { packageId -> navController.navigate("detail/$packageId") },
                onNavigateToSearch = { navController.navigate("search") },
                onBellClick = { navController.navigate("notifications") },
                onSignOut = { navController.navigate("signin") { popUpTo(navController.graph.startDestinationId) { inclusive = true } } },
                onNavigateToAccountDetails = { navController.navigate("account_detail") },
                onNavigateToSettings = { navController.navigate("setting") },
                onNavigateToRecentlyViewed = { navController.navigate("recentlyViewed") },
                onNavigateToWishlist = { navController.navigate("wishlist") },
            )
        }

        composable(
            route = "detail/{packageId}",
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ){
            PackageDetailScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(route = "search"){
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onPackageClick = { packageId -> navController.navigate("detail/$packageId") }
            )
        }

        composable("notifications") {
            NotificationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSendClick = { navController.navigate("notificationScheduler") },
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

        composable("notificationScheduler") {
            NotificationSchedulerScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("wishlist") {
            WishlistScreen(
                onNavigateBack = { navController.popBackStack() },
                onPackageClick = { packageId -> navController.navigate("detail/$packageId") }
            )
        }

        composable("recentlyViewed") {
            RecentlyViewedScreen(
                onNavigateBack = { navController.popBackStack() },
                onPackageClick = { packageId -> navController.navigate("detail/$packageId") }
            )
        }

        composable("account_detail") {
            val viewModel: AccountDetailsViewModel = hiltViewModel()
            AccountDetailsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable("setting"){
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToChangePassword = { navController.navigate("changePassword") },
                onNavigateToAboutUs = { navController.navigate("aboutus") },
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("changePassword"){
            val viewModel: ChangePasswordViewModel = hiltViewModel()
            ChangePasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("aboutus"){
            val viewModel: AboutUsViewModel = hiltViewModel()
            AboutUsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable("admin_dashboard") {
            val viewModel: AdminDashboardViewModel = hiltViewModel()
            AdminDashboardScreen(
                viewModel = viewModel,
                onNavigateToUsers = { navController.navigate("admin_users") },
                onNavigateToBookings = { navController.navigate("admin_bookings") },
                onNavigateToAnalytics = { navController.navigate("admin_analytics") },
                onNavigateToSettings = { navController.navigate("admin_settings") },
                onSignOut = {
                    navController.navigate("signin") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun PhoneContainerScreen(
    onNavigateToDetail: (String) -> Unit,
    onBellClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAccountDetails : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onNavigateToRecentlyViewed : () -> Unit,
    onNavigateToWishlist : () -> Unit,
) {
    val contentNavController = rememberNavController()
    Scaffold(
        bottomBar = { EnhancedBottomNavigationBar(navController = contentNavController) }
    ) { innerPadding ->
        NavHost(
            navController = contentNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onPackageClick = onNavigateToDetail,
                    onNavigateToSearch = onNavigateToSearch,
                    onBellClick = onBellClick
                )
            }
            composable("explore") { ExploreScreen(onPackageClick = onNavigateToDetail) }
            composable("bookings") { PlaceholderScreen(screenName = "Bookings") }
            composable("profile") {
                ProfileScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToAccountDetails = onNavigateToAccountDetails,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
                    onNavigateToWishlist = onNavigateToWishlist,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun TabletContainerScreen(
    onNavigateToDetail: (String) -> Unit,
    onBellClick: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAccountDetails : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onNavigateToRecentlyViewed : () -> Unit,
    onNavigateToWishlist : () -> Unit,
) {
    val contentNavController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    if (isLandscape) {
        Row {
            TabletSideNavigation(
                modifier = Modifier.width(240.dp).fillMaxHeight(),
                navController = contentNavController
            )
            NavHost(
                navController = contentNavController,
                startDestination = "home",
                modifier = Modifier.weight(1f)
            ) {
                composable("home") {
                    HomeScreen(
                        onPackageClick = onNavigateToDetail,
                        onNavigateToSearch = onNavigateToSearch,
                        onBellClick = onBellClick
                    )
                }
                composable("explore") { ExploreScreen(onPackageClick = onNavigateToDetail) }
                composable("bookings") { PlaceholderScreen(screenName = "Bookings") }
                composable("favorites") { PlaceholderScreen(screenName = "Favorites") }
                composable("settings") { PlaceholderScreen(screenName = "Settings") }

                composable("profile") {
                    ProfileScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToAccountDetails = onNavigateToAccountDetails,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
                        onNavigateToWishlist = onNavigateToWishlist,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    } else {
        // Portrait tablet uses the same layout as the phone, passing the new parameters through
        PhoneContainerScreen(
            onNavigateToDetail = onNavigateToDetail,
            onBellClick = onBellClick,
            onNavigateToSearch = onNavigateToSearch,
            onSignOut = onSignOut,
            onNavigateToAccountDetails = onNavigateToAccountDetails,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
            onNavigateToWishlist = onNavigateToWishlist,
        )
    }
}


private data class SideNavItem(
    val label: String,
    val route: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
)

@Composable
private fun TabletSideNavigation(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val items = listOf(
        SideNavItem("Home", "home", Icons.Outlined.Home, Icons.Filled.Home),
        SideNavItem("Explore", "explore", Icons.Outlined.Explore, Icons.Filled.Explore),
        SideNavItem("Bookings", "bookings", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
        SideNavItem("Favorites", "favorites", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
        SideNavItem("Profile", "profile", Icons.Outlined.Person, Icons.Filled.Person),
        SideNavItem("Settings", "settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.TravelExplore,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Odyssey",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    val isSelected = currentRoute == item.route

                    Surface(
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSelected) item.filledIcon else item.outlinedIcon,
                                contentDescription = item.label,
                                tint = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                item.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "John Doe",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Traveler",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(screenName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$screenName Screen", style = MaterialTheme.typography.headlineMedium)
    }
}