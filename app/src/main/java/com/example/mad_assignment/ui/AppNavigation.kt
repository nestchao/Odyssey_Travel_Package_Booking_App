package com.example.mad_assignment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.mad_assignment.R
import com.example.mad_assignment.data.model.User
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
import com.example.mad_assignment.ui.management.ManagementScreen
import com.example.mad_assignment.ui.managetravelpackage.manageTravelPackageScreen
import com.example.mad_assignment.ui.managetrip.ManageTripScreen
import com.example.mad_assignment.ui.notifications.NotificationDetailsScreen
import com.example.mad_assignment.ui.notifications.NotificationSchedulerScreen
import com.example.mad_assignment.ui.notifications.NotificationsScreen
import com.example.mad_assignment.ui.packagedetail.PackageDetailScreen
import com.example.mad_assignment.ui.profile.ProfileScreen
import com.example.mad_assignment.ui.recentlyviewed.RecentlyViewedScreen
import com.example.mad_assignment.ui.search.SearchScreen
import com.example.mad_assignment.ui.settings.SettingsScreen
import com.example.mad_assignment.ui.settings.SettingsViewModel
import com.example.mad_assignment.ui.signin.SignInScreen
import com.example.mad_assignment.ui.signin.SignInViewModel
import com.example.mad_assignment.ui.signup.SignUpScreen
import com.example.mad_assignment.ui.signup.SignUpViewModel
import com.example.mad_assignment.ui.wishlist.WishlistScreen
import com.example.mad_assignment.ui.cart.CartScreen
import com.example.mad_assignment.ui.checkout.CheckoutScreen
import com.google.gson.Gson
import com.example.mad_assignment.ui.cart.CartScreen

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
                onNavigateToSignUp = { navController.navigate("signup") },
                onForgotPassword = { navController.navigate("forgot_password") }
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
            val mainViewModel: MainViewModel = hiltViewModel()
            PhoneContainerScreen(
                mainViewModel = mainViewModel,
                onNavigateToDetail = { packageId -> navController.navigate("detail/$packageId") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToManagement = { navController.navigate("manage") },
                onBellClick = { navController.navigate("notifications") },
                onSignOut = { navController.navigate("signin") { popUpTo(navController.graph.startDestinationId) { inclusive = true } } },
                onNavigateToAccountDetails = { navController.navigate("account_detail") },
                onNavigateToSettings = { navController.navigate("setting") },
                onNavigateToRecentlyViewed = { navController.navigate("recentlyViewed") },
                onNavigateToWishlist = { navController.navigate("wishlist") },
                onNavigateToCart = { navController.navigate("cart") }
            )
        }

        composable("tablet_main") { navBackStackEntry ->
            val mainViewModel: MainViewModel = hiltViewModel()
            TabletContainerScreen(
                mainViewModel = mainViewModel,
                onNavigateToDetail = { packageId -> navController.navigate("detail/$packageId") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToCart = { navController.navigate("cart") }
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToManagement = { navController.navigate("manage") },
                onBellClick = { navController.navigate("notifications") },
                onSignOut = { navController.navigate("signin") { popUpTo(navController.graph.startDestinationId) { inclusive = true } } },
                onNavigateToAccountDetails = { navController.navigate("account_detail") },
                // Pass lambdas for navigation *from* the settings screen
                onNavigateToChangePassword = { navController.navigate("changePassword") },
                onNavigateToAboutUs = { navController.navigate("aboutus") },
                onNavigateToSettings = { navController.navigate("setting") }, // This is unused in tablet but kept for consistency
                onNavigateToRecentlyViewed = { navController.navigate("recentlyViewed") },
                onNavigateToWishlist = { navController.navigate("wishlist") },
                onNavigateToCart = { navController.navigate("cart") },
            )
        }

        composable(
            route = "detail/{packageId}",
            arguments = listOf(navArgument("packageId") { type = NavType.StringType })
        ){
            PackageDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToCheckout = { packageId, departureId, paxCountsJson ->
                    // We need to encode the JSON string so it can be safely passed in a URL
                    val encodedPaxCounts = java.net.URLEncoder.encode(paxCountsJson, "UTF-8")
                    navController.navigate(
                        "checkout?packageId=$packageId&departureId=$departureId&paxCountsJson=$encodedPaxCounts"
                    )
                }
            )
        }

        composable(route = "search"){
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onPackageClick = { packageId -> navController.navigate("detail/$packageId") }
            )
        }

        composable("cart") {
            CartScreen(
                onBackClick = { navController.popBackStack() },
                onPackageDetailsClick = { cartItem -> navController.navigate("detail/${cartItem.packageId}") },
                onPackagesClick = { navController.navigate("home") },
                onCheckoutClick = { cartId, selectedItemIds ->
                    val selectedItemIdsJson = Gson().toJson(selectedItemIds)
                    val encodedSelectedItemIds = java.net.URLEncoder.encode(selectedItemIdsJson, "UTF-8")
                    navController.navigate("checkout?cartId=$cartId&selectedItemIdsJson=$encodedSelectedItemIds")
                }
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

        composable("manage") {
            ManagementScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPackage = { navController.navigate("add_edit_package") },
                onNavigateToEditPackage = { packageId -> navController.navigate("add_edit_package?packageId=$packageId") },
                onNavigateToAddTrip = { navController.navigate("add_edit_trip") },
                onNavigateToEditTrip = { tripId -> navController.navigate("add_edit_trip?tripId=$tripId") }
            )
        }

        composable(
            route = "add_edit_package?packageId={packageId}",
            arguments = listOf(navArgument("packageId") { type = NavType.StringType; nullable = true })
        ) {
            manageTravelPackageScreen(navController = navController)
        }

        composable(
            route = "add_edit_trip?tripId={tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType; nullable = true })
        ) {
            ManageTripScreen(navController = navController)
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

        composable(
            route = "checkout?packageId={packageId}&departureId={departureId}&paxCountsJson={paxCountsJson}&cartId={cartId}&selectedItemIdsJson={selectedItemIdsJson}",
            arguments = listOf(
                navArgument("packageId") { type = NavType.StringType; nullable = true },
                navArgument("departureId") { type = NavType.StringType; nullable = true },
                navArgument("paxCountsJson") { type = NavType.StringType; nullable = true },
                navArgument("cartId") { type = NavType.StringType; nullable = true },
                navArgument("selectedItemIdsJson") { type = NavType.StringType; nullable = true }
            )
        ) {
            CheckoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    navController.navigate(mainDestination) {
                        popUpTo(mainDestination) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
private fun PhoneContainerScreen(
    mainViewModel: MainViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManagement: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAccountDetails : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToRecentlyViewed: () -> Unit,
    onNavigateToCart: () -> Unit,
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
            sharedAppGraph(
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToManagement = onNavigateToManagement,
                onBellClick = onBellClick,
                onNavigateToCart = onNavigateToCart,
                onNavigateToProfile = { contentNavController.navigate("profile") }
            )

            composable("profile") {
                ProfileScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToAccountDetails = onNavigateToAccountDetails,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToWishlist = onNavigateToWishlist,
                    onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun TabletContainerScreen(
    mainViewModel: MainViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManagement: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToAccountDetails : () -> Unit,
    onNavigateToSettings : () -> Unit,
    onBellClick: () -> Unit,
    onNavigateToWishlist: () -> Unit,
    onNavigateToRecentlyViewed: () -> Unit,
    onNavigateToCart: () -> Unit,
    // Add these lambdas to handle navigation from the settings screen
    onNavigateToChangePassword: () -> Unit,
    onNavigateToAboutUs: () -> Unit,
) {
    val user by mainViewModel.currentUser.collectAsState()
    val contentNavController = rememberNavController()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            TabletSideNavigation(
                modifier = Modifier.width(280.dp).fillMaxHeight(),
                navController = contentNavController,
                user = user
            )
            NavHost(
                navController = contentNavController,
                startDestination = "home",
                modifier = Modifier.weight(1f)
            ) {
                sharedAppGraph(
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToManagement = onNavigateToManagement,
                    onBellClick = onBellClick,
                    onNavigateToCart = onNavigateToCart,
                    onNavigateToProfile = { contentNavController.navigate("profile") { launchSingleTop = true } }
                )
                composable("wishlist") {
                    WishlistScreen(
                        onNavigateBack = { contentNavController.popBackStack() },
                        onPackageClick = onNavigateToDetail
                    )
                }
                composable("settings") {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToChangePassword = onNavigateToChangePassword,
                        onNavigateToAboutUs = onNavigateToAboutUs,
                        onNavigateBack = { contentNavController.popBackStack() },
                    )
                }

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
        // Fallback to phone layout for portrait tablets
        PhoneContainerScreen(
            mainViewModel = mainViewModel,
            onNavigateToDetail = onNavigateToDetail,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToManagement = onNavigateToManagement,
            onSignOut = onSignOut,
            onNavigateToAccountDetails = onNavigateToAccountDetails,
            onNavigateToSettings = onNavigateToSettings,
            onBellClick = onBellClick,
            onNavigateToWishlist = onNavigateToWishlist,
            onNavigateToRecentlyViewed = onNavigateToRecentlyViewed,
            onNavigateToCart = onNavigateToCart,
        )
    }
}

private fun NavGraphBuilder.sharedAppGraph(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToManagement: () -> Unit,
    onBellClick: () -> Unit, // <<< FIX IS HERE
    onNavigateToCart: () -> Unit,
    // Add this to handle profile navigation from within the shared graph
    onNavigateToProfile: () -> Unit,
) {
    composable("home") {
        HomeScreen(
            onPackageClick = onNavigateToDetail,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToManagement = onNavigateToManagement,
            onBellClick = onBellClick,
            onNavigateToCart = onNavigateToCart,
            onNavigateToProfile = onNavigateToProfile // Pass it down
        )
    }
    composable("explore") { ExploreScreen(onPackageClick = onNavigateToDetail) }
    composable("bookings") { PlaceholderScreen(screenName = "Bookings") }
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
    navController: NavController,
    user: User?
) {
    val items = listOf(
        SideNavItem("Home", "home", Icons.Outlined.Home, Icons.Filled.Home),
        SideNavItem("Explore", "explore", Icons.Outlined.Explore, Icons.Filled.Explore),
        SideNavItem("Bookings", "bookings", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
        SideNavItem("Wishlist", "wishlist", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
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
                    Image(
                        painter = painterResource(id = R.drawable.odyssey_logo),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
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
                            text = if (user != null) "${user.firstName} ${user.lastName}" else "Loading...",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = user?.userType?.name?.replaceFirstChar { it.uppercase() } ?: "Traveler",
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