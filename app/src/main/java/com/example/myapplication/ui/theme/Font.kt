package com.example.myapplication.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.myapplication.R

/**
 * App-wide display font.
 *
 * The font actually wanted was "FlechaM" (Flecha, Medium optical size) —
 * a commercial serif by Rui Abreu / R-Typography used on brave.com/ai.
 * It isn't free to embed without a purchased license, so this is a close
 * free stand-in: Zilla Slab (SIL Open Font License), which shares Flecha's
 * mechanical, square-terminal serif character. Swap this FontFamily for a
 * real Flecha one later if a license is bought — nothing else needs to
 * change, since every screen reads its font through MaterialTheme.typography.
 *
 * License text (SIL OFL 1.1) ships at the project root as
 * NOTICE-ZillaSlab-OFL.txt for attribution.
 */
val ZillaSlab = FontFamily(
    Font(R.font.zilla_slab_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.zilla_slab_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.zilla_slab_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.zilla_slab_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.zilla_slab_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.zilla_slab_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.zilla_slab_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.zilla_slab_bold_italic, FontWeight.Bold, FontStyle.Italic)
)
