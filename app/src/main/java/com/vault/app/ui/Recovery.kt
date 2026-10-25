package com.vault.app.ui

import android.content.Context
import java.io.File

/** Recovery key = random 256-bit key that wraps a second copy of the data key. VAULT never stores the key itself. */
object Recovery {
    private const val A = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private fun f(c: Context) = File(c.filesDir, "recovery.bin")
    fun exists(c: Context) = f(c).exists()

    fun encode(b: ByteArray): String {
        val sb = StringBuilder(); var buf = 0; var bits = 0
        for (x in b) { buf = (buf shl 8) or (x.toInt() and 0xFF); bits += 8
            while (bits >= 5) { sb.append(A[(buf shr (bits - 5)) and 31]); bits -= 5 }
            buf = buf and ((1 shl bits) - 1) }
        if (bits > 0) sb.append(A[(buf shl (5 - bits)) and 31])
        return sb.toString()
    }
    fun decode(s: String): ByteArray? {
        var buf = 0; var bits = 0; val out = ArrayList<Byte>()
        for (ch in s.uppercase().filter { it in A }) { buf = (buf shl 5) or A.indexOf(ch); bits += 5
            if (bits >= 8) { out.add(((buf shr (bits - 8)) and 0xFF).toByte()); bits -= 8; buf = buf and ((1 shl bits) - 1) } }
        return if (out.size >= 32) out.take(32).toByteArray() else null
    }
    /** Creates (or replaces) the recovery key. Returns the key text to show ONCE. */
    fun create(c: Context): String? {
        val dek = Session.dekCopy() ?: return null
        val key = Crypto.newDek()
        Crypto.write(f(c), Crypto.seal(key, dek))
        dek.fill(0)
        val s = encode(key); key.fill(0)
        return s.chunked(4).joinToString("-")
    }
    fun restore(c: Context, text: String): ByteArray? {
        val key = decode(text) ?: return null
        return try { Crypto.open(key, f(c).readBytes()) } catch (e: Exception) { null } finally { key.fill(0) }
    }
}
