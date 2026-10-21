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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.myapplication.data.ReleaseInfo
import com.example.myapplication.data.update.IntegrityVerifier
import com.example.myapplication.data.update.UpdatePlan
import com.example.myapplication.data.update.UpdateState
import com.example.myapplication.ui.components.glass
import dev.chrisbanes.haze.HazeState

/**
 * Interactive glassmorphic update dialog supporting on-device delta updates,
 * real-time progress reporting, and automatic full-package fallback.
 */
@Composable
fun UpdateAvailableOverlay(
    release: ReleaseInfo?,
    hazeState: HazeState,
    updateViewModel: UpdateViewModel? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(28.dp)

    val updateState by (updateViewModel?.updateState
        ?: kotlinx.coroutines.flow.MutableStateFlow(UpdateState.Idle)).collectAsState()

    AnimatedVisibility(
        visible = release != null,
        enter = fadeIn(animationSpec = tween(220)),
        exit = fadeOut(animationSpec = tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        // Only allow dismiss when not actively downloading or patching
                        if (updateState is UpdateState.Idle ||
                            updateState is UpdateState.Available ||
                            updateState is UpdateState.Failed ||
                            updateState is UpdateState.ReadyToInstall
                        ) {
                            onDismiss()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (release != null) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .fillMaxWidth(0.88f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* absorb taps so the card itself never dismisses */ }
                        .clip(cardShape)
                        .glass(
                            hazeState = hazeState,
                            shape = cardShape,
                            radius = 26.dp,
                            tintAlpha = 0.16f,
                            borderAlpha = 0.38f
                        )
                        .padding(24.dp)
                ) {
                    when (val state = updateState) {
                        is UpdateState.Downloading -> {
                            Text(
                                text = if (state.isDelta) "Downloading Delta Patch" else "Downloading Update",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.statusMessage,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { state.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "${state.progressPercent}% complete",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        is UpdateState.Verifying, is UpdateState.Patching -> {
                            val msg = if (state is UpdateState.Verifying) state.message else (state as UpdateState.Patching).message
                            Text(
                                text = "Applying Update",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF38BDF8),
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = msg,
                                    color = Color.White.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        is UpdateState.ReadyToInstall -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Ready to Install",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (state.isDeltaApplied)
                                    "Delta patch verified and applied successfully! Tap Install to update."
                                else
                                    "Update package verified and ready to install.",
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
                                    onClick = { updateViewModel?.installPendingUpdate() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Install Now")
                                }
                            }
                        }

                        is UpdateState.Failed -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Update Failed",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.errorMessage,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text("Dismiss", color = Color.White.copy(alpha = 0.75f))
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
                                    Text("GitHub")
                                }
                            }
                        }

                        else -> {
                            // Default / Available state
                            val plan = (state as? UpdateState.Available)?.plan ?: release.plan

                            Text(
                                text = "Update available",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Version ${release.versionTag} is out on GitHub.",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(10.dp))

                            // Bandwidth Savings Badge
                            if (plan is UpdatePlan.Delta) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Delta Patch: ${IntegrityVerifier.formatBytes(plan.patch.patchSizeBytes)} (${plan.savingsPercent}% saved!)",
                                        color = Color(0xFFE0F2FE),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else if (plan is UpdatePlan.Full) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Full Package: ${IntegrityVerifier.formatBytes(plan.manifest.fullApk.sizeBytes)}",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

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
                                        if (updateViewModel != null && plan != null) {
                                            updateViewModel.startUpdate()
                                        } else {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, release.releaseUrl.toUri()))
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.25f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(if (plan is UpdatePlan.Delta) "Quick Update" else "Update Now")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
