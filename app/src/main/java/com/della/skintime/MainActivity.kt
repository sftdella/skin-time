package com.della.skintime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.della.skintime.ui.analytics.AnalyticsScreen
import com.della.skintime.ui.calendar.CalendarScreen
import com.della.skintime.ui.home.HomeScreen
import com.della.skintime.ui.routine.ManageActivitiesScreen
import com.della.skintime.ui.routine.ManageChecklistScreen
import com.della.skintime.ui.routine.RoutineScreen
import com.della.skintime.ui.theme.*

sealed class Screen(val route: String) {
    object Home             : Screen("home")
    object Routine          : Screen("routine")
    object Calendar         : Screen("calendar")
    object ManageChecklist  : Screen("manage_checklist")
    object ManageActivities : Screen("manage_activities")
    object Analytics        : Screen("analytics")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkinTimeTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val currentEntry  by navController.currentBackStackEntryAsState()
    val currentRoute  = currentEntry?.destination?.route

    val bottomBarRoutes = listOf(Screen.Home.route, Screen.Routine.route, Screen.Calendar.route)
    val showBottomBar   = currentRoute in bottomBarRoutes

    Scaffold(
        containerColor = PinkBackground,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onHomeClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onRoutineClick = {
                        navController.navigate(Screen.Routine.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onCalendarClick = {
                        navController.navigate(Screen.Calendar.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Routine.route) {
                RoutineScreen(
                    onManageChecklist  = { navController.navigate(Screen.ManageChecklist.route) },
                    onManageActivities = { navController.navigate(Screen.ManageActivities.route) }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                )
            }
            composable(Screen.ManageChecklist.route) {
                ManageChecklistScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ManageActivities.route) {
                ManageActivitiesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: String?,
    onHomeClick: () -> Unit,
    onRoutineClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    NavigationBar(
        containerColor = White,
        tonalElevation = 8.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        // Home
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick  = onHomeClick,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (currentRoute == Screen.Home.route) PinkPrimary else CalEmpty
                    )
                )
            },
            label = {
                Text(
                    text = "Home",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = if (currentRoute == Screen.Home.route) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (currentRoute == Screen.Home.route) PinkPrimary else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor    = PinkSoft,
                selectedIconColor = PinkPrimary
            )
        )

        // Routine
        NavigationBarItem(
            selected = currentRoute == Screen.Routine.route,
            onClick  = onRoutineClick,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_routine),
                    contentDescription = "Routine",
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (currentRoute == Screen.Routine.route) PinkPrimary else CalEmpty
                    )
                )
            },
            label = {
                Text(
                    text = "Routine",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = if (currentRoute == Screen.Routine.route) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (currentRoute == Screen.Routine.route) PinkPrimary else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor    = PinkSoft,
                selectedIconColor = PinkPrimary
            )
        )

        // Calendar
        NavigationBarItem(
            selected = currentRoute == Screen.Calendar.route,
            onClick  = onCalendarClick,
            icon = {
                Image(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = "Calendar",
                    modifier = Modifier.size(24.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                        if (currentRoute == Screen.Calendar.route) PinkPrimary else CalEmpty
                    )
                )
            },
            label = {
                Text(
                    text = "Calendar",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = if (currentRoute == Screen.Calendar.route) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp,
                    color = if (currentRoute == Screen.Calendar.route) PinkPrimary else TextSecondary
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor    = PinkSoft,
                selectedIconColor = PinkPrimary
            )
        )
    }
}
