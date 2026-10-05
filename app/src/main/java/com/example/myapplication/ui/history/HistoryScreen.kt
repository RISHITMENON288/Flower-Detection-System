package com.example.myapplication.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.glass
import dev.chrisbanes.haze.HazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders only its own foreground content (no background paint) — the
 * black + blooming-flower backdrop is shared and drawn once behind the
 * whole nav host by MainScaffold. [hazeState] is that same shared
 * background's haze source, passed down so the glass panel below blurs the
 * real, persistent flowers — same treatment as Explore's list and search
 * bar, and the nav bar pill.
 *
 * The panel itself is size-adaptive: with few entries it shrinks to fit
 * just those rows (no stretched empty glass taking up the whole screen);
 * once entries fill the available height it stops growing and the list
 * scrolls inside it instead.
 */
@Composable
fun HistoryScreen(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val panelShape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // The panel never grows past the space actually available on
        // screen — beyond that it caps here and the list scrolls instead.
        val maxPanelHeight = maxHeight

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .heightIn(max = maxPanelHeight)
                .clip(panelShape)
                .glass(
                    hazeState = hazeState,
                    shape = panelShape,
                    radius = 24.dp,
                    tintAlpha = 0.08f,
                    borderAlpha = 0.25f
                )
        ) {
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Flowers you identify will show up here",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn(
                    // No fillMaxHeight here on purpose: without it, a
                    // LazyColumn sizes itself to its actual content height
                    // (sum of rows) up to the heightIn max below, then
                    // becomes scrollable once content exceeds that cap —
                    // which is exactly "grow with the data, stop at the
                    // screen edge".
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxPanelHeight),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                bitmap = entry.thumbnail.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = entry.topPrediction.label.replaceFirstChar { it.uppercase() },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(entry.topPrediction.confidence * 100).toInt()}% • " +
                                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                                            .format(Date(entry.timestampMillis)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
