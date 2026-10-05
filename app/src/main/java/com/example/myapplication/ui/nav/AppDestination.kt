package com.example.myapplication.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Order here is the LEFT-TO-RIGHT display order in the bottom nav bar:
 * Search (Explore) -> Home -> History.
 *
 * Home still loads first when the app opens — MainScaffold's NavHost uses
 * AppDestination.HOME.route as its startDestination explicitly, so display
 * order and start destination are independent.
 */
enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    EXPLORE("explore", "Explore", Icons.Filled.Search),
    HOME("home", "Home", Icons.Filled.Home),
    HISTORY("history", "History", Icons.Filled.List)
}
