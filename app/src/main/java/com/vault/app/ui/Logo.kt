package com.vault.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/** VAULT mark: a rounded enclosure holding a V, with a champagne key-dot above it. */
@Composable fun VaultMark(size: Dp, modifier: Modifier = Modifier) {
    val c = vc
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension; val sw = s * 0.07f
        drawRoundRect(Brush.linearGradient(listOf(c.accent, c.accent.copy(alpha = 0.5f)), Offset.Zero, Offset(s, s)),
            Offset(sw / 2, sw / 2), Size(s - sw, s - sw), CornerRadius(s * 0.28f), style = Stroke(sw))
        val v = Path().apply { moveTo(s * 0.31f, s * 0.42f); lineTo(s * 0.5f, s * 0.72f); lineTo(s * 0.69f, s * 0.42f) }
        drawPath(v, c.text, style = Stroke(sw * 1.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(c.accent, s * 0.055f, Offset(s * 0.5f, s * 0.27f))
    }
}
