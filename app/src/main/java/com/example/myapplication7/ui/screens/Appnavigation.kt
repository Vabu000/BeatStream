package com.example.myapplication7.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication7.ui.viewmodels.BeatStreamViewModel
import com.example.myapplication7.ui.viewmodels.LocationViewModel

// ЛР №9: Маршрути навігації
// ЛР №12: Додано вкладку «Локація та фото»
sealed class NavRoute(val route: String, val label: String, val icon: ImageVector) {
    object Concerts : NavRoute("concert_list", "Концерти", Icons.Default.List)
    object Location : NavRoute("location", "Локація", Icons.Default.LocationOn)
}

private val topLevelRoutes = listOf(NavRoute.Concerts, NavRoute.Location)

@Composable
fun AppNavigation(viewModel: BeatStreamViewModel) {
    val navController = rememberNavController()
    val locationViewModel = androidx.lifecycle.viewmodel.compose.viewModel<LocationViewModel>()

    // Визначаємо поточний маршрут для відображення BottomBar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelRoutes.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(NavRoute.Concerts.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Concerts.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            // Вкладка 1: Список концертів
            composable(NavRoute.Concerts.route) {
                ConcertListTab(
                    viewModel = viewModel,
                    onConcertClick = { id -> navController.navigate("concert_detail/$id") },
                    onAddClick = { navController.navigate("add_concert") }
                )
            }

            // Екран деталей
            composable(
                route = "concert_detail/{concertId}",
                arguments = listOf(navArgument("concertId") { type = NavType.IntType })
            ) { backStackEntry ->
                val concertId = backStackEntry.arguments?.getInt("concertId") ?: return@composable
                DetailScreen(
                    concertId = concertId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Екран додавання концерту
            composable("add_concert") {
                AddConcertScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // ЛР №12: Вкладка 2 — Локація та фото
            composable(NavRoute.Location.route) {
                LocationScreen(viewModel = locationViewModel)
            }
        }
    }
}
