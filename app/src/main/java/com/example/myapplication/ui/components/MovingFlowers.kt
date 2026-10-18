package com.example.myapplication.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// Each bud/bloom pair for a color. Order must stay in sync across the two lists.
private val budDrawables = listOf(
    R.drawable.bud_red,
    R.drawable.bud_white,
    R.drawable.bud_yellow,
)
private val bloomDrawables = listOf(
    R.drawable.flower_rose_red,
    R.drawable.flower_camellia_white,
    R.drawable.flower_ranunculus_yellow,
)

// How many blooms are alive at once, and how big each one gets at full
// bloom (dp). Each entry is one independent, endlessly-looping slot.
private val slotSizesDp = listOf(150f, 130f, 150f, 140f, 140f, 150f, 150f, 120f)

// Fractional screen rect the button sits in — slots avoid spawning their
// center here so blooms don't land on top of it. Tweak to match your
// button's actual position/size if you move it.
private const val KEEP_CLEAR_X_MIN = 0.14f
private const val KEEP_CLEAR_X_MAX = 0.86f
private const val KEEP_CLEAR_Y_MIN = 0.44f
private const val KEEP_CLEAR_Y_MAX = 0.62f

private fun randomPosition(rng: Random): Pair<Float, Float> {
    repeat(20) {
        val x = 0.10f + rng.nextFloat() * 0.80f
        val y = 0.06f + rng.nextFloat() * 0.88f
        val inKeepClear = x in KEEP_CLEAR_X_MIN..KEEP_CLEAR_X_MAX &&
                y in KEEP_CLEAR_Y_MIN..KEEP_CLEAR_Y_MAX
        if (!inKeepClear) return x to y
    }
    return 0.10f to 0.10f // fallback, practically never hit
}

/**
 * Flowers that quietly bloom and fade away, one lifecycle at a time, each
 * time picking a brand new random spot on screen (clear of the login
 * button) and a random color (red / white / yellow).
 */
@Composable
fun MovingFlowers(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        if (containerWidthPx > 0f && containerHeightPx > 0f) {
            slotSizesDp.forEachIndexed { index, sizeDp ->
                BloomSlot(seed = index + 1, baseSizeDp = sizeDp, containerWidthPx, containerHeightPx)
            }
        }
    }
}

@Composable
private fun BloomSlot(seed: Int, baseSizeDp: Float, containerWidthPx: Float, containerHeightPx: Float) {
    val scale = remember { Animatable(0.25f) }
    val budAlpha = remember { Animatable(0f) }
    val bloomAlpha = remember { Animatable(0f) }
    var budRes by remember { mutableIntStateOf(budDrawables[0]) }
    var bloomRes by remember { mutableIntStateOf(bloomDrawables[0]) }
    var xFrac by remember { mutableFloatStateOf(0.5f) }
    var yFrac by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(seed) {
        val rng = Random(seed)
        while (true) {
            val colorIndex = rng.nextInt(budDrawables.size)
            budRes = budDrawables[colorIndex]
            bloomRes = bloomDrawables[colorIndex]
            val (px, py) = randomPosition(rng)
            xFrac = px
            yFrac = py

            val growMs = 1500 + rng.nextInt(600)   // bud fades/grows in
            val openMs = 350 + rng.nextInt(200)    // crossfades bud -> full bloom
            val holdMs = 1800 + rng.nextInt(1400)  // stays fully bloomed
            val fadeMs = 1400 + rng.nextInt(600)   // bloom fades away
            val gapMs = 1200 + rng.nextInt(2200)   // invisible pause before next cycle

            scale.snapTo(0.25f)
            budAlpha.snapTo(0f)
            bloomAlpha.snapTo(0f)

            // bud grows in
            coroutineScope {
                launch { scale.animateTo(1f, tween(growMs, easing = FastOutSlowInEasing)) }
                launch { budAlpha.animateTo(1f, tween(growMs, easing = LinearEasing)) }
            }

            // bud opens into the full bloom, with a small pop
            coroutineScope {
                launch {
                    scale.animateTo(1.07f, tween(openMs / 2, easing = FastOutSlowInEasing))
                    scale.animateTo(1f, tween(openMs / 2, easing = FastOutSlowInEasing))
                }
                launch { budAlpha.animateTo(0f, tween(openMs, easing = LinearEasing)) }
                launch { bloomAlpha.animateTo(1f, tween(openMs, easing = LinearEasing)) }
            }

            delay(holdMs.toLong())

            // slowly disappears
            coroutineScope {
                launch { scale.animateTo(0.8f, tween(fadeMs, easing = FastOutSlowInEasing)) }
                launch { bloomAlpha.animateTo(0f, tween(fadeMs, easing = LinearEasing)) }
            }

            delay(gapMs.toLong())
        }
    }

    Box(
        modifier = Modifier
            .size(baseSizeDp.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                val sizePx = baseSizeDp.dp.toPx()
                translationX = containerWidthPx * xFrac - sizePx / 2f
                translationY = containerHeightPx * yFrac - sizePx / 2f
            }
    ) {
        Image(
            painter = painterResource(id = budRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = budAlpha.value }
        )
        Image(
            painter = painterResource(id = bloomRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = bloomAlpha.value }
        )
    }
}