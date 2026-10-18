package com.example.myapplication.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.SpeciesThumbnails
import com.example.myapplication.ml.Prediction
import com.example.myapplication.ui.components.glass
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Renders only its own foreground content (no background paint) — the
 * black + blooming-flower backdrop is shared and drawn once behind the
 * whole nav host by MainScaffold, so it stays continuous across tab
 * switches instead of restarting every time this screen is navigated to.
 * [hazeState] is that same shared background's haze source, passed down so
 * the result popup below can blur the real, persistent flowers — same
 * glass-card-over-scrim treatment as Explore's species detail popup.
 */
@Composable
fun HomeScreen(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cardShape = RoundedCornerShape(32.dp)

    // Shows the result popup the moment a fresh classification lands, then
    // stays dismissed until the next one — onImageSelected always resets
    // predictions to empty before a new result comes in, so this reliably
    // re-fires per classification rather than once ever.
    var showResultCard by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.predictions) {
        if (uiState.predictions.isNotEmpty()) showResultCard = true
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.onImageSelected(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) cameraLauncher.launch(null)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        // PickVisualMedia only hands back a Uri — decoding it (reading the
        // file twice: once for bounds, once for the sampled bitmap) is
        // blocking file I/O plus JPEG/PNG decode work, so it's dispatched
        // onto Dispatchers.IO instead of running inline on this callback,
        // which fires on the main thread. (TakePicturePreview's camera
        // path, below, already hands back a decoded Bitmap from a separate
        // process, so there's no decode to move there.)
        uri?.let {
            coroutineScope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    decodeSampledBitmap(context, it, maxDimension = 1024)
                }
                bmp?.let { decoded -> viewModel.onImageSelected(decoded) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Adaptive top inset: statusBarsPadding() reads the actual
                // status bar height for whatever device this runs on (tall
                // notch, punch-hole camera, plain bar — whatever the OS
                // reports), rather than a fixed dp guess that's wrong on some
                // phones. The uniform padding below it then adds a bit of
                // breathing room on top of that, same as every other edge.
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Identify a flower",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = uiState.selectedImage
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Selected flower",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "Take a photo or pick one from your gallery",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(24.dp)
                    )
                }

                if (uiState.isClassifying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (hasCameraPermission) {
                            cameraLauncher.launch(null)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Choose from Gallery")
                }
            }

            uiState.error?.let { error ->
                Spacer(Modifier.height(16.dp))
                Text(error, color = Color(0xFFFF8A80))
            }

            if (uiState.predictions.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Results",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    uiState.predictions.forEachIndexed { index, prediction ->
                        PredictionRow(prediction, isTop = index == 0)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        // Result popup: same glass rounded-square card over a dark scrim as
        // Explore's species detail popup, but showing the photo just
        // classified and its top prediction instead of a library thumbnail.
        // Tapping the scrim (anywhere outside the card) dismisses it;
        // tapping the card itself does nothing, so it doesn't fall through
        // and dismiss itself.
        val topPrediction = uiState.predictions.firstOrNull()
        AnimatedVisibility(
            visible = showResultCard && topPrediction != null,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showResultCard = false },
                contentAlignment = Alignment.Center
            ) {
                val prediction = topPrediction
                if (prediction != null) {
                    // The dataset's own reference photo for this species —
                    // same source Explore's species popup uses — not the
                    // photo the user just took or picked.
                    val thumbnailRes = SpeciesThumbnails.forName(prediction.label)
                    Column(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .fillMaxWidth(0.8f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* absorb taps so the card itself never dismisses */ }
                            .clip(cardShape)
                            .glass(
                                hazeState = hazeState,
                                shape = cardShape,
                                radius = 28.dp,
                                tintAlpha = 0.14f,
                                borderAlpha = 0.35f
                            )
                            .padding(20.dp)
                    ) {
                        if (thumbnailRes != null) {
                            Image(
                                painter = painterResource(id = thumbnailRes),
                                contentDescription = prediction.label,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        Text(
                            text = prediction.label.replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${(prediction.confidence * 100).toInt()}% confidence",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionRow(prediction: Prediction, isTop: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = prediction.label.replaceFirstChar { it.uppercase() },
                color = Color.White,
                fontWeight = if (isTop) FontWeight.Bold else FontWeight.Normal
            )
            Text(text = "${(prediction.confidence * 100).toInt()}%", color = Color.White.copy(alpha = 0.85f))
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = prediction.confidence,
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

/**
 * Downsamples a gallery image before decoding it fully, so a 12MP photo
 * doesn't get loaded at full resolution just to be resized down to 224px
 * by the classifier anyway.
 */
private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    val resolver = context.contentResolver

    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

    var sampleSize = 1
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
}
