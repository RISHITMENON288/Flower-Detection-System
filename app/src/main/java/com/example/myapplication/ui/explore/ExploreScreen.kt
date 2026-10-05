package com.example.myapplication.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.FlowerLabels
import com.example.myapplication.data.SpeciesThumbnails
import com.example.myapplication.ui.components.glass
import dev.chrisbanes.haze.HazeState

/**
 * Renders only its own foreground content (no background paint) — the
 * black + blooming-flower backdrop is shared and drawn once behind the
 * whole nav host by MainScaffold. [hazeState] is that same shared
 * background's haze source, passed down so the search field, list panel
 * and the species detail popup below all blur the real, persistent
 * flowers — same treatment as the nav bar pill.
 */
@Composable
fun ExploreScreen(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = viewModel()
) {
    val query by viewModel.query.collectAsState()
    val names by viewModel.filteredNames.collectAsState()

    val searchBarShape = RoundedCornerShape(50)
    val listShape = RoundedCornerShape(24.dp)
    val cardShape = RoundedCornerShape(32.dp)

    var selectedName by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Flower library",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${names.size} of ${viewModel.totalCount} species the model can recognize",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text("Search species") },
                singleLine = true,
                shape = searchBarShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = Color.White.copy(alpha = 0.8f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .glass(
                        hazeState = hazeState,
                        shape = searchBarShape,
                        radius = 20.dp,
                        tintAlpha = 0.12f,
                        borderAlpha = 0.3f
                    )
            )
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(listShape)
                    .glass(
                        hazeState = hazeState,
                        shape = listShape,
                        radius = 24.dp,
                        tintAlpha = 0.08f,
                        borderAlpha = 0.25f
                    )
                    .padding(horizontal = 8.dp)
            ) {
                items(names) { name ->
                    ListItem(
                        headlineContent = {
                            Text(name.replaceFirstChar { it.uppercase() }, color = Color.White)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedName = name }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                }
            }
        }

        // Species detail popup: a glass rounded-square card over a dark
        // scrim, covering the screen. Tapping the scrim (anywhere outside
        // the card) closes it; tapping the card itself does nothing, so it
        // doesn't fall through and dismiss itself.
        val name = selectedName
        AnimatedVisibility(
            visible = name != null,
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
                    ) { selectedName = null },
                contentAlignment = Alignment.Center
            ) {
                if (name != null) {
                    val thumbnailRes = SpeciesThumbnails.forName(name)
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
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                        Text(
                            text = name.replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Species ${FlowerLabels.NAMES.indexOf(name) + 1} of ${FlowerLabels.NAMES.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
