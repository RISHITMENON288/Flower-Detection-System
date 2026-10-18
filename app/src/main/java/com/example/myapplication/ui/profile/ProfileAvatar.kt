package com.example.myapplication.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * The small round profile picture shown in the top-right corner. Shows the
 * signed-in Google account's photo, or the first letter of their name if the
 * photo is missing or still loading. Tapping it opens the Profile screen.
 */
@Composable
fun ProfileAvatarButton(
    name: String?,
    photoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Profile" }
    ) {
        ProfilePhoto(name = name, photoUrl = photoUrl, requestedPx = 128, initialSize = 18.sp)
    }
}

/** The photo filling its parent, or the name's first letter while there is no photo. */
@Composable
internal fun ProfilePhoto(
    name: String?,
    photoUrl: String?,
    requestedPx: Int,
    initialSize: TextUnit
) {
    val image = rememberRemoteImage(sizedPhotoUrl(photoUrl, requestedPx))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name?.trim()?.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = initialSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Google profile photo URLs end in something like "=s96-c" (96px, cropped
// square). Asking for the size we actually need keeps small circles light and
// the big one sharp.
private fun sizedPhotoUrl(url: String?, px: Int): String? {
    if (url.isNullOrBlank()) return null
    return url.replace(Regex("=s[0-9]+-c"), "=s$px-c")
}

// Simple in-memory cache so the Profile screen does not re-download the
// photo the corner circle already fetched.
private val photoCache = HashMap<String, ImageBitmap>()

@Composable
private fun rememberRemoteImage(url: String?): ImageBitmap? {
    val image by produceState<ImageBitmap?>(
        initialValue = url?.let { photoCache[it] },
        key1 = url
    ) {
        if (url == null) {
            value = null
            return@produceState
        }
        val cached = photoCache[url]
        if (cached != null) {
            value = cached
            return@produceState
        }
        val loaded = withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                connection.getInputStream().use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        if (loaded != null) photoCache[url] = loaded
        value = loaded
    }
    return image
}
