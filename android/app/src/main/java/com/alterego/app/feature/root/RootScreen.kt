package com.alterego.app.feature.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alterego.app.core.design.LocalPersonaColors
import com.alterego.app.feature.lock.LockScreen

@Composable
fun RootScreen(state: RootState, viewModel: RootViewModel) {
    val colors = LocalPersonaColors.current

    if (state.locked) {
        LockScreen(mode = state.lockMode, onVerifyPin = viewModel::verifyPin, onUnlocked = viewModel::unlock)
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in TAB_ROUTES

    Scaffold(
        containerColor = colors.background,
        bottomBar = { if (showBottomBar) BottomBar(navController = navController, currentRoute = currentRoute) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp),
        ) {
            AlterEgoNavHost(
                navController = navController,
                startDestination = if (state.onboarded) Destinations.TODAY else Destinations.ONBOARDING,
                rootState = state,
                onAnniversaryAcknowledged = viewModel::acknowledgeAnniversary,
            )
        }
    }
}


private val TAB_ROUTES = setOf(Destinations.TODAY, Destinations.JOURNEY, Destinations.ME)

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    val colors = LocalPersonaColors.current
    NavigationBar(containerColor = colors.surface, contentColor = colors.onBackground) {
        Tab(navController, currentRoute, Destinations.TODAY, "Today") { Icon(Icons.Outlined.WbSunny, null) }
        Tab(navController, currentRoute, Destinations.JOURNEY, "Journey") { Icon(Icons.Outlined.Timeline, null) }
        Tab(navController, currentRoute, Destinations.ME, "Me") { Icon(Icons.Outlined.Person, null) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Tab(
    navController: NavHostController,
    currentRoute: String?,
    route: String,
    label: String,
    icon: @Composable () -> Unit,
) {
    val colors = LocalPersonaColors.current
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = icon,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = colors.accent,
            selectedTextColor = colors.accent,
            unselectedIconColor = colors.muted,
            unselectedTextColor = colors.muted,
            indicatorColor = Color.Transparent,
        ),
    )
}
