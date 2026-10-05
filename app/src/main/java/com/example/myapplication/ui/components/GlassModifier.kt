package com.example.myapplication.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
fun Modifier.glass(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(50),
    radius: Dp = 24.dp,
    tintAlpha: Float = 0.1f,
    borderAlpha: Float = 0.3f
): Modifier {
    val isPreview = LocalInspectionMode.current
    return this
        .clip(shape)
        .hazeEffect(state = hazeState) {
            if (isPreview) {
                blurEnabled = false
            }
            blurRadius = radius
            tints = listOf(HazeTint(Color.White.copy(alpha = tintAlpha)))
        }
        .border(1.dp, Color.White.copy(alpha = borderAlpha), shape)
}