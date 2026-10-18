package com.example.myapplication.ui.update

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.myapplication.data.ReleaseInfo
import com.example.myapplication.ui.components.glass
import dev.chrisbanes.haze.HazeState

/**
 * Same glass-popup treatment as Explore's species detail card: a dark
 * scrim over a centered rounded card, blurring the shared background.
 * Shown once per session when UpdateViewModel finds a newer GitHub
 * release. Tapping the scrim (outside the card) dismisses it, same as
 * the species popup.
 */
@Composable
fun UpdateAvailableOverlay(
    release: ReleaseInfo?,
    hazeState: HazeState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(28.dp)

    AnimatedVisibility(
        visible = release != null,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            if (release != null) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth(0.85f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* absorb taps so the card itself never dismisses */ }
                        .clip(cardShape)
                        .glass(
                            hazeState = hazeState,
                            shape = cardShape,
                            radius = 26.dp,
                            tintAlpha = 0.14f,
                            borderAlpha = 0.35f
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Update available",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "A new version (${release.versionTag}) is out on GitHub.",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Later", color = Color.White.copy(alpha = 0.75f))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, release.releaseUrl.toUri()))
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            )
                        ) {
                            Text("View & install")
                        }
                    }
                }
            }
        }
    }
}
