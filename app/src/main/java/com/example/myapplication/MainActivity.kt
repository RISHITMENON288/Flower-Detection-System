package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.auth.AuthViewModel
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.ui.nav.MainScaffold
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // Google sign-in (Firebase Auth). Firebase remembers the user
                // between launches, so a returning user goes straight to the
                // app. This is decided once, when the activity starts.
                val authViewModel: AuthViewModel = viewModel()
                val user by authViewModel.user.collectAsState()
                val isSigningIn by authViewModel.loading.collectAsState()
                val signInError by authViewModel.error.collectAsState()
                val context = LocalContext.current
                val startDestination = remember {
                    if (authViewModel.user.value != null) "main" else "login"
                }

                // Sign-in succeeded -> open the app. Signed out -> back to login.
                LaunchedEffect(user) {
                    val route = navController.currentDestination?.route
                    if (user != null && route == "login") {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else if (user == null && route == "main") {
                        navController.navigate("login") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                }

                // A slow, continuous "ease-out" curve (fast at first, then a
                // long gentle settle) instead of Material's default
                // FastOutSlowIn, which snaps at the start and reads as a
                // quick cut rather than something flowing. This is the same
                // family of curve used for things like a drop settling on
                // water — motion that arrives and keeps gently drifting
                // rather than stopping sharply.
                val streamEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    // Login -> Main happens once per session, so it can
                    // afford to be slow and a little indulgent: a long fade
                    // with a gentle upward drift and grow, like the new
                    // screen is rising up through the flowers rather than
                    // just appearing.
                    enterTransition = {
                        fadeIn(animationSpec = tween(750, easing = streamEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(750, easing = streamEasing)) +
                            slideInVertically(
                                initialOffsetY = { it / 10 },
                                animationSpec = tween(750, easing = streamEasing)
                            )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(550, easing = streamEasing)) +
                            scaleOut(targetScale = 1.04f, animationSpec = tween(550, easing = streamEasing))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(750, easing = streamEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(750, easing = streamEasing)) +
                            slideInVertically(
                                initialOffsetY = { -it / 10 },
                                animationSpec = tween(750, easing = streamEasing)
                            )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(550, easing = streamEasing)) +
                            scaleOut(targetScale = 1.04f, animationSpec = tween(550, easing = streamEasing))
                    }
                ) {
                    composable("login") {
                        LoginScreen(
                            isLoading = isSigningIn,
                            errorMessage = signInError,
                            onLoginClick = { authViewModel.signInWithGoogle(context) }
                        )
                    }
                    composable("main") {
                        MainScaffold(
                            userName = user?.displayName,
                            userEmail = user?.email,
                            userPhotoUrl = user?.photoUrl?.toString(),
                            onSignOut = { authViewModel.signOut(context) }
                        )
                    }
                }
            }
        }
    }
}
