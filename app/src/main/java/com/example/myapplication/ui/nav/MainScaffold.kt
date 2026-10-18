package com.example.myapplication.ui.nav

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.components.ScreenBackground
import com.example.myapplication.ui.components.glass
import com.example.myapplication.ui.dashboard.DashboardViewModel
import com.example.myapplication.ui.explore.ExploreScreen
import com.example.myapplication.ui.history.HistoryScreen
import com.example.myapplication.ui.home.HomeScreen
import com.example.myapplication.ui.profile.ProfileAvatarButton
import com.example.myapplication.ui.profile.ProfileScreen
import com.example.myapplication.ui.update.UpdateAvailableOverlay
import com.example.myapplication.ui.update.UpdateViewModel
import dev.chrisbanes.haze.HazeState

// Tab-switch transition: Material's "Fade Through" pattern — the standard,
// non-directional crossfade for destinations with no hierarchy or visual
// relationship to each other, which is exactly what Explore/Home/History
// are (unordered sibling tabs, not a forward/back flow). A short pure fade
// takes the outgoing screen out, then the incoming one fades + scales up
// very slightly, starting just before the first one finishes — a brief
// overlap so it reads as one continuous handoff rather than a hard swap,
// without two full screens visibly cross-dissolving on top of each other.
// Same three numbers for every destination, in both directions, so timing
// never varies by which tab you came from.
private const val TAB_EXIT_DURATION_MS = 160
private const val TAB_ENTER_DELAY_MS = 90
private const val TAB_ENTER_DURATION_MS = 300
private const val TAB_ENTER_INITIAL_SCALE = 0.96f

// The Profile screen is a destination of its own (opened from the avatar in the
// top-right corner), not one of the three bottom-bar tabs.
private const val PROFILE_ROUTE = "profile"

@Composable
fun MainScaffold(
    userName: String? = null,
    userEmail: String? = null,
    userPhotoUrl: String? = null,
    onSignOut: () -> Unit = {}
) {
    val navController = rememberNavController()

    // The black + blooming-flower backdrop lives here, ONCE, behind the
    // NavHost — not inside each screen. Tab switches only cross-fade the
    // small foreground content (text, buttons, lists); the backdrop itself
    // keeps running underneath, uninterrupted, which is what actually makes
    // the switch feel like one continuous app rather than separate screens
    // replacing each other. It also means every glass surface (the nav
    // pill, Explore's search bar and list) blurs the same persistent
    // background instance instead of each screen's own fresh copy.
    // Checks GitHub for a newer release once per app session. The check
    // itself runs in the background (see UpdateViewModel) — it never
    // blocks anything here, it just surfaces a dismissible overlay if a
    // newer version turns up.
    val updateViewModel: UpdateViewModel = viewModel()

    // One dashboard loader for the whole main screen, so opening and closing
    // the Profile screen does not re-download everything from GitHub each time.
    val dashboardViewModel: DashboardViewModel = viewModel()
    val availableUpdate by updateViewModel.availableUpdate.collectAsState()
    LaunchedEffect(Unit) { updateViewModel.checkForUpdateOnce() }

    ScreenBackground(modifier = Modifier.fillMaxSize()) { hazeState ->
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

        // Profile circle, top-right on the main tabs. Drawn BEFORE the NavHost
        // so each screen's own popups and scrims (result card, species card)
        // cover it like everything else. Tapping it opens the Profile screen,
        // where the circle itself is not needed.
        if (currentRoute != PROFILE_ROUTE) {
            ProfileAvatarButton(
                name = userName,
                photoUrl = userPhotoUrl,
                onClick = {
                    navController.navigate(PROFILE_ROUTE) { launchSingleTop = true }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
            )
        }

        NavHost(
            navController = navController,
            // Home is still the actual landing screen even though it isn't
            // first in the nav bar's left-to-right order below.
            startDestination = AppDestination.HOME.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(
                    animationSpec = tween(
                        TAB_ENTER_DURATION_MS,
                        delayMillis = TAB_ENTER_DELAY_MS,
                        easing = LinearOutSlowInEasing
                    )
                ) + scaleIn(
                    initialScale = TAB_ENTER_INITIAL_SCALE,
                    animationSpec = tween(
                        TAB_ENTER_DURATION_MS,
                        delayMillis = TAB_ENTER_DELAY_MS,
                        easing = LinearOutSlowInEasing
                    )
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(TAB_EXIT_DURATION_MS, easing = FastOutLinearInEasing))
            },
            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(
                        TAB_ENTER_DURATION_MS,
                        delayMillis = TAB_ENTER_DELAY_MS,
                        easing = LinearOutSlowInEasing
                    )
                ) + scaleIn(
                    initialScale = TAB_ENTER_INITIAL_SCALE,
                    animationSpec = tween(
                        TAB_ENTER_DURATION_MS,
                        delayMillis = TAB_ENTER_DELAY_MS,
                        easing = LinearOutSlowInEasing
                    )
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(TAB_EXIT_DURATION_MS, easing = FastOutLinearInEasing))
            }
        ) {
            composable(AppDestination.HOME.route) { HomeScreen(hazeState = hazeState) }
            composable(AppDestination.HISTORY.route) { HistoryScreen(hazeState = hazeState) }
            composable(AppDestination.EXPLORE.route) { ExploreScreen(hazeState = hazeState) }
            composable(PROFILE_ROUTE) {
                ProfileScreen(
                    userName = userName,
                    userEmail = userEmail,
                    userPhotoUrl = userPhotoUrl,
                    hazeState = hazeState,
                    dashboardViewModel = dashboardViewModel,
                    onBack = { navController.popBackStack() },
                    onSignOut = onSignOut
                )
            }
        }

        // Floating glass-pill nav bar, lifted off the bottom edge.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .glass(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(50),
                    radius = 28.dp,
                    tintAlpha = 0.12f,
                    borderAlpha = 0.3f
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AppDestination.entries order is the display order: Explore, Home, History.
            AppDestination.entries.forEach { destination ->
                val selected = currentRoute == destination.route
                NavPill(
                    destination = destination,
                    selected = selected,
                    hazeState = hazeState,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        UpdateAvailableOverlay(
            release = availableUpdate,
            hazeState = hazeState,
            onDismiss = { updateViewModel.dismiss() }
        )
    }
}

@Composable
private fun NavPill(
    destination: AppDestination,
    selected: Boolean,
    hazeState: HazeState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .glass(
                hazeState = hazeState,
                shape = RoundedCornerShape(50),
                radius = 20.dp,
                tintAlpha = if (selected) 0.22f else 0.06f,
                borderAlpha = if (selected) 0.45f else 0.12f
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = if (selected) 18.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = Color.White.copy(alpha = if (selected) 1f else 0.6f),
            modifier = Modifier.size(22.dp)
        )
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = destination.label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
