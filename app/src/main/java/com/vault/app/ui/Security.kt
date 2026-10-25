package com.vault.app.ui

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Key hierarchy: a hardware-backed Keystore key (KEK) that requires biometric/PIN for EVERY use
 * wraps a random 256-bit data key (DEK). The DEK encrypts the vault file with AES-256-GCM.
 * The DEK exists in memory only while unlocked and is zeroed on lock. No custom crypto: platform AES-GCM only.
 */
object Crypto {
    private const val ALIAS = "vault_kek_v1"
    private const val T = "AES/GCM/NoPadding"
    private val AAD = "vault-v1".toByteArray()
    private fun ks() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun wrapped(c: Context) = File(c.filesDir, "wrapped.bin")
    fun dataFile(c: Context) = File(c.filesDir, "vault.dat")
    fun hasVault(c: Context) = wrapped(c).exists()

    private fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256)
        .setUserAuthenticationRequired(true)
        .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL) // auth on every use
        .setIsStrongBoxBacked(strongBox).build()

    private fun createKek() {
        ks().deleteEntry(ALIAS)
        fun gen(sb: Boolean) = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec(sb)) }.generateKey()
        try { gen(true) } catch (e: StrongBoxUnavailableException) { gen(false) }
    }

    fun encryptCipher(): Cipher { createKek(); return Cipher.getInstance(T).apply { init(Cipher.ENCRYPT_MODE, ks().getKey(ALIAS, null) as SecretKey) } }
    fun decryptCipher(c: Context): Cipher {
        val iv = wrapped(c).readBytes().copyOfRange(0, 12)
        return Cipher.getInstance(T).apply { init(Cipher.DECRYPT_MODE, ks().getKey(ALIAS, null) as SecretKey, GCMParameterSpec(128, iv)) }
    }
    fun saveWrapped(c: Context, iv: ByteArray, ct: ByteArray) = write(wrapped(c), iv + ct)
    fun wrappedCt(c: Context): ByteArray = wrapped(c).readBytes().let { it.copyOfRange(12, it.size) }
    fun newDek() = ByteArray(32).also { SecureRandom().nextBytes(it) }

    fun seal(dek: ByteArray, plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance(T).apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(dek, "AES"), GCMParameterSpec(128, iv)); updateAAD(AAD) }
        return iv + c.doFinal(plain)
    }
    fun open(dek: ByteArray, blob: ByteArray): ByteArray { // throws if tampered
        val c = Cipher.getInstance(T).apply { init(Cipher.DECRYPT_MODE, SecretKeySpec(dek, "AES"), GCMParameterSpec(128, blob.copyOfRange(0, 12))); updateAAD(AAD) }
        return c.doFinal(blob, 12, blob.size - 12)
    }
    /** Atomic write: temp file then rename, so a crash never leaves a half-written vault. */
    fun write(f: File, b: ByteArray) {
        val t = File(f.path + ".tmp"); t.writeBytes(b)
        if (!t.renameTo(f)) { t.delete(); throw IOException("write failed") }
    }
}

/** Decrypted data lives here only while unlocked. lock() zeroes the key and drops everything. */
object Session {
    var docs by mutableStateOf(emptyList<Doc>()); private set
    var contacts by mutableStateOf(emptyList<Contact>()); private set
    private var dek: ByteArray? = null

    fun unlock(c: Context, key: ByteArray) {
        val f = Crypto.dataFile(c)
        dek = key
        if (f.exists()) {
            val j = JSONObject(String(Crypto.open(key, f.readBytes())))
            docs = j.getJSONArray("docs").let { a -> List(a.length()) { toDoc(a.getJSONObject(it)) } }
            contacts = j.getJSONArray("contacts").let { a -> List(a.length()) { toContact(a.getJSONObject(it)) } }
        } else { docs = seedDocs; contacts = seedContacts; persist(c) }
    }
    fun dekCopy() = dek?.copyOf()
    fun addDoc(c: Context, d: Doc) { docs = listOf(d) + docs; persist(c) }
    fun removeDoc(c: Context, d: Doc) { d.file?.let { File(c.filesDir, it).delete() }; docs = docs - d; persist(c) }
    fun addContact(c: Context, x: Contact) { contacts = contacts + x; persist(c) }
    fun saveFile(c: Context, b: ByteArray): String {
        val k = dek ?: error("locked"); val n = "f_" + java.util.UUID.randomUUID() + ".enc"
        Crypto.write(File(c.filesDir, n), Crypto.seal(k, b)); return n
    }
    fun readFile(c: Context, n: String): ByteArray? = try { dek?.let { Crypto.open(it, File(c.filesDir, n).readBytes()) } } catch (e: Exception) { null }
    fun lock() { dek?.fill(0); dek = null; docs = emptyList(); contacts = emptyList() }
    fun persist(c: Context) {
        val k = dek ?: return
        val j = JSONObject().put("docs", JSONArray(docs.map { fromDoc(it) })).put("contacts", JSONArray(contacts.map { fromContact(it) }))
        Crypto.write(Crypto.dataFile(c), Crypto.seal(k, j.toString().toByteArray()))
    }
    private fun JSONObject.s(k: String) = if (has(k)) getString(k) else null
    private fun toDoc(o: JSONObject) = Doc(o.getString("t"), Category.valueOf(o.getString("c")), o.s("m"), o.s("r"), o.s("e"), if (o.has("d")) o.getInt("d") else null, o.getString("a"), o.s("f"))
    private fun fromDoc(d: Doc) = JSONObject().put("t", d.title).put("c", d.category.name).put("m", d.masked as Any?).put("r", d.revealed as Any?)
        .put("e", d.expiry as Any?).put("d", d.daysLeft as Any?).put("a", d.accessed).put("f", d.file as Any?)
    private fun toContact(o: JSONObject) = Contact(o.getString("n"), o.getString("g"), o.getString("p"), o.optBoolean("x"))
    private fun fromContact(c: Contact) = JSONObject().put("n", c.name).put("g", c.group).put("p", c.phone).put("x", c.emergency)
}

object Prefs {
    var idleSec by mutableIntStateOf(60)
    @Volatile var last = System.nanoTime()
    fun touch() { last = System.nanoTime() }
    fun idleExceeded() = (System.nanoTime() - last) / 1_000_000_000L >= idleSec
}

/** Best-effort only: a determined attacker can hide these signs. Shown as a notice, never as proof of safety. */
object Integrity {
    fun warnings(c: Context): List<String> = buildList {
        if (Build.TAGS?.contains("test-keys") == true) add("custom firmware")
        if (listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su").any { File(it).exists() }) add("root binary found")
        if (Debug.isDebuggerConnected()) add("debugger attached")
        if ((c.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) add("debug build")
    }
}

/** Fresh authentication before a sensitive action (e.g. revealing an identifier). */
fun reauth(a: FragmentActivity, title: String, ok: () -> Unit) {
    val p = BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) = ok()
    })
    p.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).build())
}
