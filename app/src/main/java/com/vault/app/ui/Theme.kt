package com.vault.app.ui

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.PlatformTextStyle

class VColors(
    val bg: Color, val surface: Color, val surface2: Color, val text: Color, val text2: Color,
    val text3: Color, val divider: Color, val accent: Color, val warning: Color, val dark: Boolean
)

val LightColors = VColors(Color(0xFFF7F7F5), Color.White, Color(0xFFF0F0ED), Color(0xFF171717),
    Color(0xFF777777), Color(0xFFA0A0A0), Color(0xFFE8E8E5), Color(0xFFB99A60), Color(0xFFB7791F), false)
val DarkColors = VColors(Color(0xFF101012), Color(0xFF19191D), Color(0xFF222226), Color(0xFFF5F5F5),
    Color(0xFFA4A4AD), Color(0xFF6E6E76), Color(0xFF303035), Color(0xFFC6A96B), Color(0xFFE0B04F), true)

val LocalVColors = staticCompositionLocalOf { LightColors }

/** Design tokens: 8-pt grid, radii, motion. */
object Tokens {
    val s4 = 4.dp; val s8 = 8.dp; val s12 = 12.dp; val s16 = 16.dp; val s24 = 24.dp; val s32 = 32.dp
    val screenH = 24.dp          // one horizontal gutter for every screen
    val rowMin = 64.dp           // one row height for every list row
    val icon = 40.dp             // icon badge in rows
    val dividerInset = 16.dp + 40.dp + 12.dp  // pad + badge + gap: divider starts under the text
    val rSmall = 12.dp; val rCard = 18.dp; val rSheet = 26.dp
    val touch = 48.dp
    const val motionMs = 300
}

object Type {
    val headline = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp)
    val section = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val title = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 24.sp)
    val meta = TextStyle(fontSize = 13.sp, lineHeight = 20.sp)
    val label = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    val wordmark = label.copy(letterSpacing = 3.sp)
}

val vc: VColors @Composable get() = LocalVColors.current

/** Returns 0 when the user disabled animations (reduced motion). */
@Composable fun motionMs(): Int {
    val ctx = LocalContext.current
    val scale = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }
    return if (scale == 0f) 0 else Tokens.motionMs
}

@Composable fun VaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVColors provides if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
