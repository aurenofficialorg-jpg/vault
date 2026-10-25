package com.vault.app.ui
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility

fun catIcon(c: Category) = when (c) {
    Category.Identity -> Icons.Outlined.Badge; Category.Education -> Icons.Outlined.School
    Category.Financial -> Icons.Outlined.AccountBalance; Category.Personal -> Icons.Outlined.Description
}

@Composable fun Page(title: String, content: LazyListScopeContent) {
    val st = rememberLazyListState()
    val t by remember { derivedStateOf { if (st.firstVisibleItemIndex > 0) 1f else (st.firstVisibleItemScrollOffset / 90f).coerceIn(0f, 1f) } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), st, contentPadding = PaddingValues(Tokens.screenH, top + Tokens.s16, Tokens.screenH, 104.dp),
            verticalArrangement = Arrangement.spacedBy(Tokens.s8)) {
            item { Text(title, style = Type.headline, color = vc.text, modifier = Modifier.padding(bottom = Tokens.s16).graphicsLayer { alpha = 1f - t; translationY = -t * 8f }) }
            content()
        }
        Box(Modifier.fillMaxWidth().height(top + 48.dp).graphicsLayer { alpha = t }.background(vc.bg.copy(alpha = 0.94f)).padding(top = top), Alignment.Center) {
            Text(title, style = Type.title, color = vc.text)
            HorizontalDivider(Modifier.align(Alignment.BottomCenter), color = vc.divider, thickness = 0.5.dp)
        }
    }
}
typealias LazyListScopeContent = androidx.compose.foundation.lazy.LazyListScope.() -> Unit

@Composable fun HomeScreen(goVault: () -> Unit, goContacts: () -> Unit) {
    val hr = java.time.LocalTime.now().hour
    val greeting = when { hr < 12 -> "Good morning."; hr < 17 -> "Good afternoon."; else -> "Good evening." }
    val expiring = remember { sampleDocs.filter { (it.daysLeft ?: 999) <= 120 }.sortedBy { it.daysLeft } }
    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(Tokens.screenH, Tokens.s16, Tokens.screenH, 104.dp),
        verticalArrangement = Arrangement.spacedBy(Tokens.s8)) {
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { VaultMark(28.dp); Spacer(Modifier.width(Tokens.s12)); Text("VAULT", style = Type.wordmark, color = vc.text) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, "Vault is locked on exit", tint = vc.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(Tokens.s4)); Text("Protected", style = Type.label, color = vc.text2)
                }
            }
            Spacer(Modifier.height(Tokens.s32))
            Text(greeting, style = Type.body, color = vc.text2)
            Text("Everything in its place.", style = Type.headline, color = vc.text)
            Text("Your important information, organized and ready.", style = Type.body, color = vc.text2, modifier = Modifier.padding(top = Tokens.s8))
            Text("Sample data shown for this prototype.", style = Type.meta, color = vc.text3, modifier = Modifier.padding(top = Tokens.s4))
        }
        item {
            SectionTitle("Quick access")
            val tiles = listOf(Triple("Identity", Icons.Outlined.Badge, goVault), Triple("Bank & Cards", Icons.Outlined.AccountBalance, goVault),
                Triple("Documents", Icons.Outlined.Description, goVault), Triple("Contacts", Icons.Outlined.Contacts, goContacts))
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.s12)) {
                tiles.chunked(2).forEach { r ->
                    Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(Tokens.s12)) {
                        r.forEach { (n, ic, go) ->
                            PressCard(Modifier.weight(1f).fillMaxHeight(), go) {
                                Column(Modifier.fillMaxWidth().padding(Tokens.s16)) { IconBadge(ic, size = Tokens.icon); Spacer(Modifier.height(Tokens.s16)); Text(n, style = Type.title, color = vc.text, maxLines = 1)
                                    val k = when (n) { "Identity" -> sampleDocs.count { it.category == Category.Identity }; "Bank & Cards" -> sampleDocs.count { it.category == Category.Financial }; "Documents" -> sampleDocs.size; else -> sampleContacts.size }
                                    Text("$k items", style = Type.meta, color = vc.text2) }
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("Recently accessed") }
        itemsIndexed(sampleDocs.take(4), key = { _, d -> "r-" + d.title }) { i, d -> Row2(catIcon(d.category), d.title, d.category.label, d.accessed, index = i, onClick = goVault) }
        if (expiring.isNotEmpty()) {
            item { SectionTitle("Upcoming reminders") }
            itemsIndexed(expiring.take(2), key = { _, d -> "e-" + d.title }) { i, d -> Row2(Icons.Outlined.EventNote, "${d.title} renewal", "Expires in ${d.daysLeft} days", null, index = i, onClick = goVault) }
        }
        item {
            SectionTitle("Security")
            PressCard(Modifier.fillMaxWidth(), {}) {
                Column(Modifier.fillMaxWidth().padding(Tokens.s16)) {
                    Text("Biometric or device-credential unlock is required to open VAULT.", style = Type.body, color = vc.text)
                    Text("Recovery method: not set up yet", style = Type.meta, color = vc.warning, modifier = Modifier.padding(top = Tokens.s4))
                }
            }
        }
    }
}

@Composable fun ContactsScreen() {
    val ctx = LocalContext.current
    var adding by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
    Page("Contacts") {
        itemsIndexed(remember { sampleContacts.sortedByDescending { it.emergency } }, key = { _, c -> c.name + c.phone }) { i, c ->
            PressCard(Modifier.fillMaxWidth(), {}, index = i) {
                Row(Modifier.heightIn(min = Tokens.rowMin).padding(horizontal = Tokens.s16, vertical = Tokens.s12), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(Tokens.icon).background(if (c.emergency) vc.accent.copy(alpha = 0.16f) else vc.surface2, CircleShape), Alignment.Center) {
                        Text(c.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""), style = Type.title, color = if (c.emergency) vc.accent else vc.text2)
                    }
                    Spacer(Modifier.width(Tokens.s12))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                        Text(c.name, style = Type.title, color = vc.text, maxLines = 1)
                        Text(if (c.emergency) "${c.group} · Emergency" else c.group, style = Type.meta, color = vc.text2)
                    }
                    RoundAction(Icons.Outlined.Call, "Call ${c.name}") { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}"))) }
                    Spacer(Modifier.width(Tokens.s8))
                    RoundAction(Icons.Outlined.Sms, "Message ${c.name}") { ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${c.phone}"))) }
                }
            }
        }
    }
    AddFab("Add contact", Modifier.align(Alignment.BottomEnd)) { adding = true }
    if (adding) AddContactSheet { adding = false }
    }
}

@Composable private fun RoundAction(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val bg by animateColorAsState(if (pr) vc.accent.copy(alpha = 0.22f) else vc.surface2, tween(100), label = "ra")
    Box(Modifier.size(Tokens.touch).clip(CircleShape).clickable(src, null, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick), Alignment.Center) {
        Box(Modifier.size(Tokens.icon).drawBehind { drawCircle(bg) })
        Icon(icon, desc, tint = vc.accent, modifier = Modifier.size(20.dp))
    }
}

@Composable private fun Group(label: String, content: @Composable ColumnScope.() -> Unit) {
    Text(label, style = Type.label, color = vc.text2, modifier = Modifier.padding(start = Tokens.s16, top = Tokens.s24, bottom = Tokens.s8))
    val shape = RoundedCornerShape(Tokens.rCard)
    Column(Modifier.fillMaxWidth().staggerIn(0).clip(shape).background(vc.surface).border(0.5.dp, vc.divider, shape), content = content)
}

@Composable private fun SRow(icon: ImageVector, title: String, sub: String, last: Boolean = false, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val bg by animateColorAsState(if (pr) vc.surface2 else vc.surface, tween(100), label = "sr")
    val h = LocalHapticFeedback.current
    Column {
        Row(Modifier.fillMaxWidth().background(bg).clickable(src, null) { h.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick() }
            .heightIn(min = Tokens.rowMin).padding(horizontal = Tokens.s16, vertical = Tokens.s12), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, size = Tokens.icon); Spacer(Modifier.width(Tokens.s12))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) { Text(title, style = Type.title, color = vc.text); Text(sub, style = Type.meta, color = vc.text2) }
            Spacer(Modifier.width(Tokens.s12)); Box(Modifier.widthIn(min = 24.dp), Alignment.CenterEnd) { trailing() }
        }
        if (!last) HorizontalDivider(Modifier.padding(start = Tokens.dividerInset), color = vc.divider, thickness = 0.5.dp)
    }
}

@Composable fun SettingsScreen() {
    var bio by remember { mutableStateOf(true) }
    var hide by remember { mutableStateOf(true) }
    var lockIdx by remember { mutableIntStateOf(1) }
    val ctx0 = LocalContext.current
    var rs by remember { mutableStateOf(false) }
    val opts = listOf("30 seconds", "1 minute", "2 minutes", "5 minutes")
    val sw = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = vc.accent, checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = Color.White, uncheckedTrackColor = vc.divider, uncheckedBorderColor = Color.Transparent)
    if (rs) { RecoverySetup(ctx0 as FragmentActivity) { rs = false }; return }
    Page("Settings") {
        item {
            Group("Security") {
                SRow(Icons.Outlined.Fingerprint, "Unlock method", "Biometric or device PIN is required every time.", onClick = {}) { Icon(Icons.Outlined.Lock, null, tint = vc.accent) }
                SRow(Icons.Outlined.NotificationsOff, "Private notifications", "Reminders never show document details.", onClick = { hide = !hide }) { Switch(hide, { hide = it }, colors = sw) }
                SRow(Icons.Outlined.Timer, "Automatic lock", "Also locks whenever you leave the app.", onClick = { lockIdx = (lockIdx + 1) % opts.size; Prefs.idleSec = listOf(30, 60, 120, 300)[lockIdx] }) {
                    AnimatedContent(lockIdx, transitionSpec = { (slideInVertically(tween(260, easing = Smooth)) { it / 3 } + fadeIn(tween(200))) togetherWith (slideOutVertically(tween(160, easing = Smooth)) { -it / 3 } + fadeOut(tween(100))) }, label = "lock") {
                        Text(opts[it], style = Type.meta, color = vc.accent)
                    }
                }
                SRow(Icons.Outlined.Key, "Recovery key", if (Recovery.exists(ctx0)) "Active. Tap to create a new one." else "Not set up. A changed fingerprint could lock you out for good.", last = true, onClick = { rs = true }) {
                    Icon(Icons.Outlined.ChevronRight, null, tint = vc.text3)
                }
            }
        }
        item {
            val ctx = LocalContext.current; val w = remember { Integrity.warnings(ctx) }
            Group("Protection") {
                SRow(Icons.Outlined.EnhancedEncryption, "Encryption", "AES-256-GCM. Key held in hardware-backed Keystore. No network access.", onClick = {}) { Icon(Icons.Outlined.CheckCircle, null, tint = vc.accent) }
                SRow(Icons.Outlined.Shield, "Device integrity", if (w.isEmpty()) "No common root or debug signs found. Best-effort check, not proof." else w.joinToString(", "), last = true, onClick = {}) {
                    Icon(if (w.isEmpty()) Icons.Outlined.CheckCircle else Icons.Outlined.Warning, null, tint = if (w.isEmpty()) vc.accent else vc.warning)
                }
            }
        }
        item { Group("Coming later") { Text("Encrypted backup, secure sharing and emergency access need a backend and a security review. They are not available in this version.", style = Type.meta, color = vc.text2, modifier = Modifier.padding(Tokens.s16)) } }
    }
}
