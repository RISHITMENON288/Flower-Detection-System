package com.example.myapplication.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.github.ChangeLogEntry
import com.example.myapplication.ui.components.glass
import com.example.myapplication.ui.dashboard.ChangeLogDetailModal
import com.example.myapplication.ui.dashboard.DashboardSection
import com.example.myapplication.ui.dashboard.DashboardViewModel
import dev.chrisbanes.haze.HazeState

/**
 * Full-screen profile page, opened from the round avatar in the top-right
 * corner. Shows the signed-in Google account (photo, name, email) and a Sign
 * out button, then the SCM dashboard (see DashboardSection) below it. The
 * page scrolls, so it can keep growing.
 *
 * Like the other tabs, it draws no background of its own: the shared flower
 * backdrop from MainScaffold shows through, and [hazeState] is what the glass
 * cards blur.
 */
@Composable
fun ProfileScreen(
    userName: String?,
    userEmail: String?,
    userPhotoUrl: String?,
    hazeState: HazeState,
    dashboardViewModel: DashboardViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(28.dp)
    var selectedChangeLogEntry by remember { mutableStateOf<ChangeLogEntry?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Profile",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))

            // Account card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(
                        hazeState = hazeState,
                        shape = cardShape,
                        radius = 26.dp,
                        tintAlpha = 0.14f,
                        borderAlpha = 0.35f
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                ) {
                    ProfilePhoto(
                        name = userName,
                        photoUrl = userPhotoUrl,
                        requestedPx = 256,
                        initialSize = 40.sp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = userName?.takeIf { it.isNotBlank() } ?: "Signed in",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!userEmail.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = userEmail,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.22f),
                        contentColor = Color.White
                    )
                ) {
                    Text("Sign out")
                }
            }

            Spacer(Modifier.height(20.dp))

            // SCM dashboard: version check, release audit, change log, dependencies.
            DashboardSection(
                hazeState = hazeState,
                viewModel = dashboardViewModel,
                onSelectChangeLogEntry = { selectedChangeLogEntry = it }
            )

            // Keeps the last card clear of the floating bottom nav bar.
            Spacer(Modifier.height(120.dp))
        }

        // Full-screen glass capsule popup for Change Log details
        val entry = selectedChangeLogEntry
        AnimatedVisibility(
            visible = entry != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (entry != null) {
                ChangeLogDetailModal(
                    entry = entry,
                    hazeState = hazeState,
                    onDismiss = { selectedChangeLogEntry = null }
                )
            }
        }
    }
}
