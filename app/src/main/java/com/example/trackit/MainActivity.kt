package com.example.trackit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trackit.ui.AppViewModelProvider
import com.example.trackit.ui.OnboardingScreen
import com.example.trackit.ui.SettingsScreen
import com.example.trackit.ui.SettingsViewModel
import com.example.trackit.ui.TourStep
import com.example.trackit.ui.TourViewModel
import com.example.trackit.ui.inventory.AddItemScreen
import com.example.trackit.ui.inventory.InventoryScreen
import com.example.trackit.ui.inventory.LocationScreen
import com.example.trackit.ui.theme.TrackITTheme
import kotlinx.coroutines.delay

sealed class Screen(val route: String, val resourceId: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Inventory : Screen("inventory_tab", R.string.home_tab, Icons.Default.Home)
    object Locations : Screen("locations", R.string.locations_title, Icons.Default.LocationOn)
    object Settings : Screen("settings", R.string.settings_title, Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val tourViewModel: TourViewModel = viewModel(factory = AppViewModelProvider.Factory)
            
            val isDarkModeEnabled by viewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
            val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
            
            val showTour by tourViewModel.showTour.collectAsStateWithLifecycle()
            val currentTourStep by tourViewModel.currentStep.collectAsStateWithLifecycle()

            TrackITTheme(darkTheme = isDarkModeEnabled) {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1000L) // Show splash for 1 second
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else if (isFirstLaunch && !showTour) { 
                    OnboardingScreen(
                        onComplete = { viewModel.completeOnboarding() },
                        onStartTour = { tourViewModel.nextStep() }
                    )
                } else {
                    TrackItApp(
                        tourStep = if (showTour) currentTourStep else null,
                        onNextTourStep = { tourViewModel.nextStep() },
                        onSkipTour = { tourViewModel.skipTour() }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TrackIT",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "by ",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )
                Text(
                    text = "SmallThings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D5A27) // Dark green matching the image
                    )
                )
            }
        }
    }
}

@Composable
fun TrackItApp(
    tourStep: TourStep? = null,
    onNextTourStep: () -> Unit = {},
    onSkipTour: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Auto-navigate to Locations tab if tour requires it
    LaunchedEffect(tourStep) {
        if (tourStep == TourStep.LOCATIONS_TAB || tourStep == TourStep.SUB_LOCATIONS) {
            if (currentDestination?.route != Screen.Locations.route) {
                navController.navigate(Screen.Locations.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else if (tourStep == TourStep.INVENTORY_OVERVIEW || tourStep == TourStep.ADD_ITEM) {
             if (currentDestination?.route != "inventory" && currentDestination?.route != Screen.Inventory.route) {
                navController.navigate(Screen.Inventory.route)
             }
        }
    }

    val items = listOf(
        Screen.Inventory,
        Screen.Locations,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Transparent
            ) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.resourceId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            val route = screen.route
                            val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true

                            if (isSelected) {
                                // Already on this tab. If we are on a sub-page, pop back to the tab's root.
                                // For the Inventory tab, the root destination is "inventory".
                                val rootRoute = if (screen == Screen.Inventory) "inventory" else route
                                if (currentDestination.route != rootRoute) {
                                    navController.popBackStack(rootRoute, inclusive = false)
                                }
                            } else {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Inventory.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            navigation(startDestination = "inventory", route = Screen.Inventory.route) {
                composable("inventory") {
                    InventoryScreen(
                        onNavigateToAddItem = { navController.navigate("add_item") },
                        tourStep = tourStep,
                        onNextStep = onNextTourStep,
                        onSkipTour = onSkipTour
                    )
                }
                composable("add_item") {
                    AddItemScreen(
                        navigateBack = { navController.popBackStack() },
                        onNavigateUp = { navController.navigateUp() }
                    )
                }
            }
            composable(Screen.Locations.route) {
                LocationScreen(
                    tourStep = tourStep,
                    onNextStep = onNextTourStep,
                    onSkipTour = onSkipTour
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackItAppPreview() {
    TrackITTheme {
        TrackItApp()
    }
}
