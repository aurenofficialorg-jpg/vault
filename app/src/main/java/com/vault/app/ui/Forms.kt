package com.vault.app.ui

import android.graphics.BitmapFactory
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun VField(label: String, v: String, on: (String) -> Unit, kb: KeyboardType = KeyboardType.Text) {
    Column(Modifier.fillMaxWidth().padding(bottom = Tokens.s16)) {
        Text(label, style = Type.label, color = vc.text2, modifier = Modifier.padding(start = Tokens.s4, bottom = Tokens.s8))
        BasicTextField(v, on, singleLine = true, textStyle = Type.body.copy(color = vc.text), cursorBrush = SolidColor(vc.accent),
            keyboardOptions = KeyboardOptions(keyboardType = kb),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).background(vc.surface2, RoundedCornerShape(Tokens.rSmall)).padding(horizontal = Tokens.s16, vertical = Tokens.s12 + 2.dp))
    }
}

@Composable fun AddFab(desc: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pr by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pr) 0.92f else 1f, snap(), label = "fab")
    Box(modifier.padding(end = Tokens.screenH, bottom = Tokens.s24).size(56.dp).graphicsLayer { scaleX = s; scaleY = s }
        .background(vc.accent, CircleShape).clip(CircleShape).clickable(src, null, role = Role.Button, onClick = onClick), Alignment.Center) {
        Icon(Icons.Outlined.Add, desc, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable fun ToastPill(msg: String?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf("") }
    if (msg != null) last = msg
    AnimatedVisibility(msg != null, modifier, enter = fadeIn(tween(200)) + slideInVertically(tween(280, easing = Smooth)) { it / 2 }, exit = fadeOut(tween(160)) + slideOutVertically(tween(160, easing = Smooth)) { it / 3 }) {
        Row(Modifier.padding(bottom = Tokens.s24).background(vc.text, RoundedCornerShape(24.dp)).padding(horizontal = Tokens.s16, vertical = Tokens.s12), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, null, tint = vc.accent, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(Tokens.s8))
            Text(last, style = Type.title, color = vc.bg)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SheetBox(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = vc.surface,
        shape = RoundedCornerShape(topStart = Tokens.rSheet, topEnd = Tokens.rSheet)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(horizontal = Tokens.s24).padding(bottom = Tokens.s24), content = content)
    }
}

private fun mask(id: String): String? = if (id.isBlank()) null else "•".repeat((id.length - 4).coerceIn(0, 8)) + id.takeLast(4)

@Composable fun AddDocFlow(onClose: (String?) -> Unit) {
    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    var stage by remember { mutableIntStateOf(0) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var title by remember { mutableStateOf("") }; var cat by remember { mutableStateOf(Category.Identity) }
    var ident by remember { mutableStateOf("") }; var expiry by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }; var err by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                val (b, name) = withContext(Dispatchers.IO) {
                    val len = ctx.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    require(len in 1..15_000_000L) { "size" }
                    val n = ctx.contentResolver.query(uri, null, null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null } ?: "Document"
                    ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() } to n
                }
                bytes = b; title = name.substringBeforeLast('.'); stage = 1; err = null
            } catch (e: Exception) { err = "That file couldn't be read, or it is larger than 15 MB." }
        }
    }
    SheetBox({ if (!saving) onClose(null) }) {
        if (stage == 0) {
            Text("Add document", style = Type.section, color = vc.text, modifier = Modifier.padding(bottom = Tokens.s16))
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.s8)) {
                Row2(Icons.Outlined.UploadFile, "Import PDF or photo", "Encrypted before it is saved", index = 0) { pick.launch(arrayOf("application/pdf", "image/*")) }
                Row2(Icons.Outlined.EditNote, "Create manual record", "Details only, no file", index = 1) { stage = 1 }
            }
            err?.let { Text(it, style = Type.meta, color = vc.warning, modifier = Modifier.padding(top = Tokens.s12)) }
        } else {
            Text("Review", style = Type.section, color = vc.text, modifier = Modifier.padding(bottom = Tokens.s16))
            VField("Title", title, { title = it })
            Text("Category", style = Type.label, color = vc.text2, modifier = Modifier.padding(start = Tokens.s4, bottom = Tokens.s8))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = Tokens.s16), horizontalArrangement = Arrangement.spacedBy(Tokens.s8)) {
                Category.values().forEach { c -> FilterChip(cat == c, { cat = c }, { Text(c.label) }) }
            }
            VField("Identifier (optional)", ident, { ident = it })
            Text("Stored encrypted and shown masked. Never enter a card PIN, CVV or OTP.", style = Type.meta, color = vc.text3, modifier = Modifier.padding(bottom = Tokens.s12))
            VField("Expiry (optional)", expiry, { expiry = it })
            err?.let { Text(it, style = Type.meta, color = vc.warning, modifier = Modifier.padding(bottom = Tokens.s8)) }
            PrimaryButton(if (saving) "Saving…" else "Save securely") {
                if (saving) return@PrimaryButton
                if (title.isBlank()) { err = "Give this document a title."; return@PrimaryButton }
                saving = true; err = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val f = bytes?.let { Session.saveFile(ctx, it) }
                            Session.addDoc(ctx, Doc(title.trim(), cat, mask(ident.trim()), ident.trim().ifBlank { null }, expiry.trim().ifBlank { null }, null, "Just now", f))
                        }
                        onClose("Saved securely")  // shown only after the encrypted write completed
                    } catch (e: Exception) { err = "Couldn't save. Your entry is kept, so you can try again."; saving = false }
                }
            }
        }
    }
}

@Composable fun AddContactSheet(onClose: () -> Unit) {
    val ctx = LocalContext.current; val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Family") }; var emer by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    SheetBox(onClose) {
        Text("New contact", style = Type.section, color = vc.text, modifier = Modifier.padding(bottom = Tokens.s16))
        VField("Name", name, { name = it }); VField("Phone number", phone, { phone = it }, KeyboardType.Phone)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = Tokens.s16), horizontalArrangement = Arrangement.spacedBy(Tokens.s8)) {
            listOf("Family", "Doctor", "Bank", "Insurance", "Other").forEach { g -> FilterChip(group == g, { group = g }, { Text(g) }) }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = Tokens.touch), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Emergency contact", style = Type.body, color = vc.text)
            Switch(emer, { emer = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = vc.accent, checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = vc.divider, uncheckedBorderColor = Color.Transparent))
        }
        err?.let { Text(it, style = Type.meta, color = vc.warning, modifier = Modifier.padding(vertical = Tokens.s8)) }
        Spacer(Modifier.height(Tokens.s8))
        PrimaryButton("Save contact") {
            when {
                name.isBlank() -> err = "Enter a name."
                !Regex("^\\+?[0-9 ]{5,15}$").matches(phone.trim()) -> err = "Enter a valid phone number."
                else -> scope.launch {
                    try { withContext(Dispatchers.IO) { Session.addContact(ctx, Contact(name.trim(), group, phone.trim().replace(" ", ""), emer)) }; onClose() }
                    catch (e: Exception) { err = "Couldn't save. Try again." }
                }
            }
        }
    }
}

@Composable fun RecoverySetup(a: FragmentActivity, close: () -> Unit) {
    val ctx = LocalContext.current
    var key by remember { mutableStateOf<String?>(null) }
    BackHandler { key = null; close() }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Tokens.screenH, vertical = Tokens.s16)) {
        IconButton({ key = null; close() }, Modifier.offset(x = (-12).dp).size(Tokens.touch)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = vc.text) }
        Spacer(Modifier.height(Tokens.s16))
        Text("Recovery key", style = Type.headline, color = vc.text)
        Text("If your phone's fingerprints change or you move to a new phone, this key is the only way back in. VAULT cannot recover it for you. Write it on paper and keep it somewhere safe. Do not screenshot it or store it on this phone. Creating a new key replaces the old one.",
            style = Type.body, color = vc.text2, modifier = Modifier.padding(vertical = Tokens.s16))
        val k = key
        if (k == null) PrimaryButton(if (Recovery.exists(ctx)) "Create a new key" else "Create recovery key") {
            reauth(a, "Create recovery key") { key = Recovery.create(ctx) }
        } else {
            PressCard(Modifier.fillMaxWidth(), {}) {
                Text(k.split("-").chunked(4).joinToString("\n") { it.joinToString("  ") }, style = Type.section.copy(fontFamily = FontFamily.Monospace, fontSize = 18.sp, lineHeight = 30.sp),
                    color = vc.text, modifier = Modifier.padding(Tokens.s24))
            }
            Text("Shown once. It cannot be displayed again.", style = Type.meta, color = vc.warning, modifier = Modifier.padding(vertical = Tokens.s12))
            PrimaryButton("I've written it down") { key = null; close() }
        }
    }
}

@Composable fun RecoveryEntry(a: FragmentActivity, cancel: () -> Unit, done: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(Tokens.screenH), Arrangement.Center) {
        Text("Enter recovery key", style = Type.headline, color = vc.text)
        Text("The 52-character key you wrote down.", style = Type.body, color = vc.text2, modifier = Modifier.padding(vertical = Tokens.s12))
        VField("Recovery key", text, { text = it.uppercase() })
        err?.let { Text(it, style = Type.meta, color = vc.warning, modifier = Modifier.padding(bottom = Tokens.s12)) }
        PrimaryButton("Recover") {
            val dek = Recovery.restore(ctx, text)
            if (dek == null) { err = "That key didn't work. Check it and try again."; return@PrimaryButton }
            try {
                val cipher = Crypto.encryptCipher() // re-wraps the data key under a fresh hardware key
                BiometricPrompt(a, ContextCompat.getMainExecutor(ctx), object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                        try { val c = r.cryptoObject!!.cipher!!; Crypto.saveWrapped(ctx, c.iv, c.doFinal(dek)); Session.unlock(ctx, dek); done() }
                        catch (e: Exception) { Session.lock(); err = "Recovery failed. Nothing was opened." }
                    }
                }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Confirm it's you").setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).build(), BiometricPrompt.CryptoObject(cipher))
            } catch (e: Exception) { err = "Set a screen lock on this device first." }
        }
        TextAction("Cancel", cancel)
    }
}
