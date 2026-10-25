package com.vault.app.ui
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tappable surface: critically damped spring press (no bounce), plus staggered entrance. */
@Composable fun PressCard(modifier: Modifier = Modifier, onClick: () -> Unit, index: Int = 0, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.98f else 1f, spring(1f, Spring.StiffnessMediumLow), label = "press")
    val h = LocalHapticFeedback.current
    Box(modifier.staggerIn(index).scale(s).shadow(if (vc.dark) 0.dp else 8.dp, RoundedCornerShape(Tokens.rCard), clip = false, ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.07f)).clip(RoundedCornerShape(Tokens.rCard)).background(vc.surface)
        .border(0.5.dp, vc.divider, RoundedCornerShape(Tokens.rCard))
        .clickable(src, null, role = Role.Button, onClick = { h.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() })) { content() }
}

@Composable fun IconBadge(icon: ImageVector, desc: String? = null, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(RoundedCornerShape(Tokens.rSmall)).background(vc.surface2), Alignment.Center) {
        Icon(icon, desc, tint = vc.accent, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable fun Row2(icon: ImageVector, title: String, sub: String?, trailing: String? = null, index: Int = 0,
                     modifier: Modifier = Modifier, iconMod: Modifier = Modifier, titleMod: Modifier = Modifier, onClick: () -> Unit = {}) {
    PressCard(modifier.fillMaxWidth(), onClick, index) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = Tokens.s16, vertical = Tokens.s12), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, modifier = iconMod)
            Spacer(Modifier.width(Tokens.s12))
            Column(Modifier.weight(1f)) {
                Text(title, style = Type.title, color = vc.text, modifier = titleMod)
                if (sub != null) Text(sub, style = Type.meta, color = vc.text2)
            }
            if (trailing != null) Text(trailing, style = Type.meta, color = vc.text3)
            Icon(Icons.Outlined.ChevronRight, null, tint = vc.text3)
        }
    }
}

@Composable fun SectionTitle(t: String) = Text(t, style = Type.section, color = vc.text, modifier = Modifier.padding(top = Tokens.s24, bottom = Tokens.s12))

@Composable fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.98f else 1f, spring(1f, Spring.StiffnessMediumLow), label = "btn")
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 52.dp).scale(s), interactionSource = src, shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (vc.dark) vc.accent else Color(0xFF292929), contentColor = if (vc.dark) vc.bg else vc.surface)) {
        Text(text, style = Type.title)
    }
}

@Composable fun TextAction(text: String, onClick: () -> Unit) =
    TextButton(onClick, Modifier.heightIn(min = Tokens.touch)) { Text(text, style = Type.body, color = vc.text2) }
