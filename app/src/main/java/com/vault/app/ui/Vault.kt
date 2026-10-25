package com.vault.app.ui
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable fun VaultScreen() {
    var open by remember { mutableStateOf<Doc?>(null) }
    var q by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<Category?>(null) }
    var loading by remember { mutableStateOf(true) }
    var adding by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(2200); toast = null } }
    LaunchedEffect(Unit) { delay(500); loading = false }  // stands in for a real DB read
    BackHandler(open != null) { open = null }
    val ms = motionMs()
    Box(Modifier.fillMaxSize()) {
    SharedTransitionLayout {
        AnimatedContent(open, transitionSpec = { fadeIn(tween(ms, easing = Smooth)) togetherWith fadeOut(tween(ms / 2, easing = Smooth)) }, label = "vault") { d ->
            if (d == null) VaultList(q, { q = it }, cat, { cat = it }, loading, { open = it }, this@SharedTransitionLayout, this)
            else DocDetail(d, { open = null }, this@SharedTransitionLayout, this)
        }
    }
    AnimatedVisibility(open == null && !loading, Modifier.align(Alignment.BottomEnd), enter = fadeIn(tween(200)) + scaleIn(glide(), 0.8f), exit = fadeOut(tween(120)) + scaleOut(tween(120), 0.8f)) { AddFab("Add document") { adding = true } }
    ToastPill(toast, Modifier.align(Alignment.BottomCenter))
    if (adding) AddDocFlow { adding = false; if (it != null) toast = it }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable private fun VaultList(q: String, onQ: (String) -> Unit, cat: Category?, onCat: (Category?) -> Unit, loading: Boolean,
                                  onOpen: (Doc) -> Unit, sts: SharedTransitionScope, avs: AnimatedVisibilityScope) {
    val res = remember(q, cat, sampleDocs) { sampleDocs.filter { (cat == null || it.category == cat) && (q.isBlank() || it.title.contains(q, true) || it.category.label.contains(q, true)) } }
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) vc.accent else Color.Transparent, tween(160), label = "focus")
    val shape = RoundedCornerShape(Tokens.rSmall)
    Page("Your Vault") {
        item {
            BasicTextField(q, onQ, singleLine = true, textStyle = Type.body.copy(color = vc.text), cursorBrush = SolidColor(vc.accent),
                modifier = Modifier.fillMaxWidth().heightIn(min = Tokens.touch).onFocusChanged { focused = it.isFocused }
                    .background(vc.surface2, shape).border(1.dp, border, shape).padding(horizontal = Tokens.s16),
                decorationBox = { inner ->
                    Row(Modifier.heightIn(min = Tokens.touch), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, "Search", tint = if (focused) vc.accent else vc.text3); Spacer(Modifier.width(Tokens.s12))
                        Box(Modifier.weight(1f)) { if (q.isEmpty()) Text("Search documents", style = Type.body, color = vc.text3); inner() }
                        AnimatedVisibility(q.isNotEmpty(), enter = fadeIn(tween(140)) + scaleIn(tween(160), 0.8f), exit = fadeOut(tween(100)) + scaleOut(tween(100), 0.8f)) {
                            IconButton({ onQ("") }) { Icon(Icons.Outlined.Close, "Clear search", tint = vc.text2) }
                        }
                    }
                })
            Row(Modifier.padding(top = Tokens.s12).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Tokens.s8)) {
                FilterChip(cat == null, { onCat(null) }, { Text("All") })
                Category.values().forEach { c -> FilterChip(cat == c, { onCat(if (cat == c) null else c) }, { Text(c.label) }) }
            }
        }
        if (loading) items(5) { Box(Modifier.fillMaxWidth().height(Tokens.rowMin).shimmer()) }
        else {
            if (res.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = Tokens.s32), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No matches", style = Type.title, color = vc.text); Text("Try a different word or clear the filter.", style = Type.meta, color = vc.text2)
                }
            }
            itemsIndexed(res, key = { _, d -> d.title }) { i, d ->
                with(sts) {
                    Row2(catIcon(d.category), d.title, d.masked ?: d.category.label, d.expiry, index = i, modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        iconMod = Modifier.sharedElement(rememberSharedContentState("i-" + d.title), avs)) { onOpen(d) }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable private fun DocDetail(d: Doc, back: () -> Unit, sts: SharedTransitionScope, avs: AnimatedVisibilityScope) {
    var reveal by remember { mutableStateOf(false) }
    val act = LocalContext.current as FragmentActivity
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(reveal) { if (reveal) { delay(15_000); reveal = false } } // auto-hide after 15 s
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = Tokens.screenH, vertical = Tokens.s16)) {
        IconButton(back, Modifier.offset(x = (-12).dp).size(Tokens.touch)) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = vc.text) }
        Spacer(Modifier.height(Tokens.s16))
        with(sts) {
            IconBadge(catIcon(d.category), size = 56.dp, modifier = Modifier.sharedElement(rememberSharedContentState("i-" + d.title), avs))
            Spacer(Modifier.height(Tokens.s16))
            Text(d.title, style = Type.headline, color = vc.text)
        }
        Row(Modifier.padding(top = Tokens.s12), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, null, tint = vc.accent, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(Tokens.s8))
            Text("${d.category.label} · Protected on this device", style = Type.meta, color = vc.text2)
        }
        Spacer(Modifier.height(Tokens.s24))
        DocPreview(d); Spacer(Modifier.height(Tokens.s16))
        PressCard(Modifier.fillMaxWidth(), {}, index = 2) {
            Column(Modifier.fillMaxWidth().padding(horizontal = Tokens.s16)) {
                if (d.masked != null) {
                    Row(Modifier.fillMaxWidth().heightIn(min = Tokens.rowMin), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(Tokens.s4)) {
                            Text("Identifier", style = Type.label, color = vc.text3)
                            AnimatedContent(reveal, transitionSpec = { fadeIn(tween(200, easing = Smooth)) togetherWith fadeOut(tween(100)) }, label = "rev") { r ->
                                Text(if (r) d.revealed ?: "" else d.masked, style = Type.title, color = vc.text)
                            }
                        }
                        TextButton({ if (reveal) reveal = false else reauth(act, "Reveal identifier") { reveal = true } }) { Text(if (reveal) "Hide" else "Reveal", color = vc.accent) }
                    }
                    HorizontalDivider(color = vc.divider, thickness = 0.5.dp)
                }
                Field("Category", d.category.label); HorizontalDivider(color = vc.divider, thickness = 0.5.dp)
                Field("Expiry", d.expiry ?: "None"); HorizontalDivider(color = vc.divider, thickness = 0.5.dp)
                Field("Last opened", d.accessed)
            }
        }
        Text("Stored encrypted on this device.", style = Type.meta, color = vc.text3, modifier = Modifier.padding(top = Tokens.s16, bottom = Tokens.s8))
        TextButton({ confirm = true }, Modifier.heightIn(min = Tokens.touch).offset(x = (-12).dp)) { Text("Delete record", color = Color(0xFFB3261E)) }
        if (confirm) AlertDialog({ confirm = false }, containerColor = vc.surface,
            title = { Text("Delete this record?", color = vc.text) },
            text = { Text("This permanently deletes the record and its encrypted file from this device. VAULT keeps no cloud copy, and this can't be undone.", color = vc.text2) },
            confirmButton = { TextButton({ Session.removeDoc(act, d); confirm = false; back() }) { Text("Delete", color = Color(0xFFB3261E)) } },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancel", color = vc.text2) } })
    }
}

@Composable private fun Field(l: String, v: String) =
    Column(Modifier.fillMaxWidth().heightIn(min = Tokens.rowMin).padding(vertical = Tokens.s12), Arrangement.spacedBy(Tokens.s4, Alignment.CenterVertically)) {
        Text(l, style = Type.label, color = vc.text3); Text(v, style = Type.title, color = vc.text)
    }

@Composable private fun DocPreview(d: Doc) {
    val ctx = LocalContext.current
    val img by produceState<ImageBitmap?>(null, d) {
        value = withContext(Dispatchers.IO) { d.file?.let { Session.readFile(ctx, it) }?.let { b ->
            BitmapFactory.decodeByteArray(b, 0, b.size, BitmapFactory.Options().apply { inSampleSize = 2; inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 })?.asImageBitmap() } }
    }
    val shape = RoundedCornerShape(Tokens.rCard)
    Box(Modifier.fillMaxWidth().height(168.dp).staggerIn(1).clip(shape).background(Brush.verticalGradient(listOf(vc.surface, vc.surface2))).border(0.5.dp, vc.divider, shape)) {
        val i = img
        if (i != null) Image(i, "Document preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else {
            Column(Modifier.padding(Tokens.s24), verticalArrangement = Arrangement.spacedBy(Tokens.s12)) {
                listOf(0.5f, 0.9f, 0.75f, 0.6f).forEach { Box(Modifier.fillMaxWidth(it).height(8.dp).background(vc.divider, RoundedCornerShape(4.dp))) }
            }
            if (d.file != null) Text("PDF or file attached · encrypted", style = Type.meta, color = vc.text2, modifier = Modifier.align(Alignment.BottomStart).padding(Tokens.s16))
        }
        VaultMark(28.dp, Modifier.align(Alignment.BottomEnd).padding(Tokens.s16))
    }
}
