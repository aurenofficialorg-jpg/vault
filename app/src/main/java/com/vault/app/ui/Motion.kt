package com.vault.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Apple-style deceleration: fast start, long soft landing. */
val Smooth = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/** Critically damped spring (no bounce) used for every touch response. */
fun <T> snap() = spring<T>(dampingRatio = 1f, stiffness = 520f)
/** Slightly softer spring for position / size changes. */
fun <T> glide() = spring<T>(dampingRatio = 1f, stiffness = 300f)

/**
 * Fade + 12dp rise, staggered. Plays once per item.
 * Uses an Animatable read ONLY inside graphicsLayer {}, so nothing recomposes while it runs:
 * the animation is applied on the render thread, not the composition thread.
 */
@Composable fun Modifier.staggerIn(index: Int): Modifier {
    val ms = motionMs()
    if (ms == 0) return this
    var done by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (done) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!done) {
            delay(index.coerceAtMost(4) * 30L)
            p.animateTo(1f, tween(ms, easing = Smooth))
            done = true
        }
    }
    return graphicsLayer { alpha = p.value; translationY = (1f - p.value) * 12.dp.toPx() }
}

/** Skeleton shimmer drawn in the draw phase only (zero recompositions). */
@Composable fun Modifier.shimmer(): Modifier {
    val c = vc
    val still = motionMs() == 0
    val x = if (still) null else rememberInfiniteTransition(label = "sh")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "x")
    val r = Tokens.rCard
    return drawBehind {
        val p = x?.value ?: 0.5f
        val w = size.width
        drawRoundRect(
            Brush.horizontalGradient(listOf(c.surface2, c.divider, c.surface2), startX = (p * 2f - 1f) * w, endX = (p * 2f - 1f) * w + w),
            cornerRadius = CornerRadius(r.toPx()))
    }
}
