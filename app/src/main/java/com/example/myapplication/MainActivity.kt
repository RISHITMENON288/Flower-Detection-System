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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                    startDestination = "login",
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
                            onLoginClick = {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("main") {
                        MainScaffold()
                    }
                }
            }
        }
    }
}
