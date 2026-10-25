package com.vault.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Emphasized-decelerate: fast start, long soft landing. Used for everything that enters. */
val Smooth = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Fade + 20dp rise, staggered by index. Plays once per item (survives scrolling and rotation). */
@Composable fun Modifier.staggerIn(index: Int): Modifier {
    val ms = motionMs()
    var go by rememberSaveable { mutableStateOf(ms == 0) }
    LaunchedEffect(Unit) { if (!go) { delay(index.coerceAtMost(8) * 45L); go = true } }
    val a by animateFloatAsState(if (go) 1f else 0f, tween(if (ms == 0) 0 else ms + 140, easing = Smooth), label = "in")
    return graphicsLayer { alpha = a; translationY = (1f - a) * 20.dp.toPx() }
}

/** Skeleton shimmer; static when animations are disabled. */
@Composable fun Modifier.shimmer(): Modifier {
    val c = vc
    val x = if (motionMs() == 0) 0.5f else
        rememberInfiniteTransition(label = "sh").animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "x").value
    return background(Brush.horizontalGradient(listOf(c.surface2, c.divider, c.surface2), startX = -300f + x * 1400f, endX = x * 1400f + 200f),
        RoundedCornerShape(Tokens.rCard))
}
