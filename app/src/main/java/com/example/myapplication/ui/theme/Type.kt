package com.example.myapplication.ui.theme

import androidx.compose.material3.Typography

// Material3's stock type scale (sizes/weights/line-heights), just with
// every style's font swapped to ZillaSlab (see Font.kt) instead of the
// system default. Any Text() in the app that doesn't set its own style —
// which is almost all of them — reads its font from here via
// MaterialTheme.typography, so this one change applies the font everywhere
// at once: login screen, nav bar, Home/History/Explore.
private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = ZillaSlab),
    displayMedium = base.displayMedium.copy(fontFamily = ZillaSlab),
    displaySmall = base.displaySmall.copy(fontFamily = ZillaSlab),
    headlineLarge = base.headlineLarge.copy(fontFamily = ZillaSlab),
    headlineMedium = base.headlineMedium.copy(fontFamily = ZillaSlab),
    headlineSmall = base.headlineSmall.copy(fontFamily = ZillaSlab),
    titleLarge = base.titleLarge.copy(fontFamily = ZillaSlab),
    titleMedium = base.titleMedium.copy(fontFamily = ZillaSlab),
    titleSmall = base.titleSmall.copy(fontFamily = ZillaSlab),
    bodyLarge = base.bodyLarge.copy(fontFamily = ZillaSlab),
    bodyMedium = base.bodyMedium.copy(fontFamily = ZillaSlab),
    bodySmall = base.bodySmall.copy(fontFamily = ZillaSlab),
    labelLarge = base.labelLarge.copy(fontFamily = ZillaSlab),
    labelMedium = base.labelMedium.copy(fontFamily = ZillaSlab),
    labelSmall = base.labelSmall.copy(fontFamily = ZillaSlab)
)
