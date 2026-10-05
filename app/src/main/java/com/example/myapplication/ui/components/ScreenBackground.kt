package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Shared backdrop for every main screen (Home, History, Explore): the same
 * black background with the blooming-flower animation used behind the
 * login button, just without the button itself.
 *
 * Also hands the screen a [HazeState] already sourced from this background,
 * so any element inside can be made to look like real frosted glass over the
 * flowers with `Modifier.glass(hazeState, ...)` from GlassModifier.kt —
 * exactly the same way the login button does it.
 *
 * Usage:
 *
 *   ScreenBackground(modifier = modifier) { hazeState ->
 *       Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
 *           // Modifier.glass(hazeState, ...) works on anything in here
 *       }
 *   }
 */
@Composable
fun ScreenBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(hazeState: HazeState) -> Unit
) {
    val hazeState = rememberHazeState()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black))
            MovingFlowers()
        }
        content(hazeState)
    }
}
