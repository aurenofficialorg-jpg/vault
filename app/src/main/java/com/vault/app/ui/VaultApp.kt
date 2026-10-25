package com.vault.app.ui
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.delay
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch

private enum class Tab(val label: String) { Home("Home"), Vault("Vault"), Contacts("Contacts"), Settings("Settings") }

@Composable fun VaultApp(activity: FragmentActivity) = VaultTheme {
    var onboarded by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(true) }
    var recovering by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { locked = true } // lock the moment the app leaves the foreground
    LaunchedEffect(locked) { if (locked) Session.lock() }          // zero the key, drop decrypted data
    LaunchedEffect(locked) { if (!locked) { Prefs.touch(); while (true) { delay(1000); if (Prefs.idleExceeded()) { locked = true; break } } } }
    val stage = if (!onboarded) 0 else if (locked) 1 else 2
    val ms = motionMs()
    Box(Modifier.fillMaxSize().background(vc.bg).pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent(PointerEventPass.Initial); Prefs.touch() } } }) {
        AnimatedContent(stage, transitionSpec = {
            fadeIn(tween(ms, ms / 4, Smooth)) togetherWith fadeOut(tween(ms / 2, easing = Smooth))
        }, label = "stage") { s ->
            when (s) { 0 -> Onboarding { onboarded = true }; 1 -> if (recovering) RecoveryEntry(activity, { recovering = false }) { recovering = false; locked = false } else LockScreen(activity, { recovering = true }) { locked = false }; else -> Shell() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun Onboarding(done: () -> Unit) {
    val pages = listOf(
        Triple("Your important things, finally organized.", "Keep your essential documents, personal records, and important contacts together in one private space.", Icons.Outlined.Folder),
        Triple("Private means private.", "Your personal information deserves serious protection. VAULT keeps your data on this device and locks when you leave.", Icons.Outlined.Lock),
        Triple("Everything important, within reach.", "Find a document, call an important contact, or check an upcoming expiry in just a few moments.", Icons.Outlined.Search))
    val st = rememberPagerState { 3 }; val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(Tokens.screenH)) {
        HorizontalPager(st, Modifier.weight(1f)) { i ->
            Column(Modifier.fillMaxSize().graphicsLayer { val pos = i - (st.currentPage + st.currentPageOffsetFraction); alpha = 1f - kotlin.math.abs(pos).coerceIn(0f, 1f); translationX = -pos * size.width * 0.3f }, Arrangement.Center) {
                Icon(pages[i].third, null, tint = vc.accent, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(Tokens.s24))
                Text(pages[i].first, style = Type.headline, color = vc.text)
                Spacer(Modifier.height(Tokens.s12))
                Text(pages[i].second, style = Type.body, color = vc.text2)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Tokens.s16), Arrangement.Center, Alignment.CenterVertically) {
            repeat(3) {
                val sel = st.currentPage == it
                val w by animateDpAsState(if (sel) 24.dp else 8.dp, glide(), label = "w")
                val c by animateColorAsState(if (sel) vc.accent else vc.divider, tween(240), label = "c")
                Box(Modifier.padding(horizontal = Tokens.s4).height(8.dp).width(w).clip(CircleShape).drawBehind { drawRect(c) })
            }
        }
        AnimatedContent(st.currentPage < 2, transitionSpec = { fadeIn(tween(220, easing = Smooth)) togetherWith fadeOut(tween(100)) }, label = "cta") { more ->
            if (more) PrimaryButton("Continue") { scope.launch { st.animateScrollToPage(st.currentPage + 1, animationSpec = spring(1f, 260f)) } }
            else Column { PrimaryButton("Create my VAULT", onClick = done); TextAction("I already have an account", done) }
        }
    }
}

@Composable private fun LockScreen(activity: FragmentActivity, onRecover: () -> Unit, unlocked: () -> Unit) {
    var error by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val first = remember { !Crypto.hasVault(ctx) }
    val warn = remember { Integrity.warnings(ctx) }
    fun prompt() {
        error = null
        val cipher = try { if (first) Crypto.encryptCipher() else Crypto.decryptCipher(ctx) }
            catch (e: KeyPermanentlyInvalidatedException) { error = "Your device biometrics changed, which invalidated this vault's key. It cannot be opened without a recovery method."; return }
            catch (e: Exception) { error = "Set a screen lock (PIN, pattern or password) on this device to use VAULT."; return }
        val p = BiometricPrompt(activity, ContextCompat.getMainExecutor(ctx), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                try {
                    val c = r.cryptoObject!!.cipher!!
                    val dek = if (first) Crypto.newDek().also { Crypto.saveWrapped(ctx, c.iv, c.doFinal(it)) } else c.doFinal(Crypto.wrappedCt(ctx))
                    Session.unlock(ctx, dek); unlocked()
                } catch (e: Exception) { Session.lock(); error = "VAULT could not verify its data, so nothing was opened." }
            }
            override fun onAuthenticationError(code: Int, msg: CharSequence) { error = msg.toString() }
        })
        p.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(if (first) "Set up VAULT" else "Unlock VAULT")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).build(), BiometricPrompt.CryptoObject(cipher))
    }
    var shown by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (shown) 1f else 0.8f, glide(), label = "lock")
    LaunchedEffect(Unit) { shown = true; prompt() }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(Tokens.screenH), Arrangement.Center, Alignment.CenterHorizontally) {
        VaultMark(80.dp, Modifier.graphicsLayer { scaleX = sc; scaleY = sc; alpha = (sc - 0.8f) * 5f })
        Spacer(Modifier.height(Tokens.s24))
        Text("VAULT is locked", style = Type.section, color = vc.text)
        error?.let { Text(it, style = Type.meta, color = vc.warning, textAlign = TextAlign.Center, modifier = Modifier.padding(top = Tokens.s8)) }
        Spacer(Modifier.height(Tokens.s24))
        if (warn.isNotEmpty()) Text("Device notice: " + warn.joinToString(", "), style = Type.meta, color = vc.warning, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = Tokens.s12))
        PrimaryButton(if (first) "Set up" else if (error == null) "Unlock" else "Try again") { prompt() }
        if (Recovery.exists(ctx)) TextAction("Use recovery key", onRecover)
    }
}

@Composable private fun Shell() {
    var tab by remember { mutableStateOf(Tab.Home) }
    val ms = motionMs()
    Scaffold(containerColor = vc.bg, bottomBar = { NavBar(tab) { tab = it } }) { pad ->
        Box(Modifier.padding(bottom = pad.calculateBottomPadding())) {
            AnimatedContent(tab, transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                fadeIn(tween(ms * 2 / 3, ms / 6, Smooth)) togetherWith fadeOut(tween(ms / 4, easing = LinearEasing))
            }, label = "tab") { t ->
                when (t) {
                    Tab.Home -> HomeScreen({ tab = Tab.Vault }, { tab = Tab.Contacts })
                    Tab.Vault -> VaultScreen()
                    Tab.Contacts -> ContactsScreen()
                    Tab.Settings -> SettingsScreen()
                }
            }
        }
    }
}

@Composable private fun NavBar(tab: Tab, onSelect: (Tab) -> Unit) {
    val h = LocalHapticFeedback.current
    Column(Modifier.background(vc.surface)) {
        HorizontalDivider(color = vc.divider, thickness = 0.5.dp)
        BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().height(64.dp)) {
            val w = maxWidth / 4
            val c2 = vc.surface2
            val x by animateFloatAsState(tab.ordinal.toFloat(), glide(), label = "pill")
            Box(Modifier.offset { androidx.compose.ui.unit.IntOffset((w.toPx() * x).toInt(), 0) }.width(w).fillMaxHeight().padding(horizontal = Tokens.s12, vertical = Tokens.s8).clip(RoundedCornerShape(16.dp)).drawBehind { drawRect(c2) })
            Row(Modifier.fillMaxSize()) {
                Tab.values().forEach { t ->
                    val sel = tab == t
                    val ic: ImageVector = when (t) { Tab.Home -> Icons.Outlined.Home; Tab.Vault -> Icons.Outlined.Folder; Tab.Contacts -> Icons.Outlined.People; Tab.Settings -> Icons.Outlined.Settings }
                    val tint by animateColorAsState(if (sel) vc.accent else vc.text3, tween(200), label = "tint")
                    val label by animateColorAsState(if (sel) vc.text else vc.text3, tween(200), label = "label")
                    val sc by animateFloatAsState(if (sel) 1.06f else 1f, snap(), label = "ic")
                    Column(Modifier.weight(1f).fillMaxHeight().selectable(sel, remember { MutableInteractionSource() }, null, role = Role.Tab) {
                        if (!sel) h.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(t)
                    }, Arrangement.Center, Alignment.CenterHorizontally) {
                        Icon(ic, null, tint = tint, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = sc; scaleY = sc })
                        Spacer(Modifier.height(Tokens.s4))
                        Text(t.label, style = Type.label, color = label, maxLines = 1)
                    }
                }
            }
        }
    }
}
