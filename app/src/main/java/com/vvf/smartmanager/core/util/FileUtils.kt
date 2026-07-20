package com.vvf.smartmanager.core.util

import android.webkit.MimeTypeMap
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FileSizeFormatter {
    private val units = arrayOf("B", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        // Locale.US explicitly — audit finding, 20 July 2026: String.format() without a Locale
        // uses the device's default, which on comma-decimal locales would render "512,0 B"
        // instead of "512.0 B". A technical byte-size string should stay locale-independent;
        // this also matters for CI, since testDebugUnitTest's assertions assume a period.
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }
}

object MimeTypeResolver {
    fun resolve(file: File): String? {
        val extension = file.extension.lowercase(Locale.ROOT)
        if (extension.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}

/** Level 1 duplicate detection — exact-content match via SHA-256. */
object Sha256Hasher {
    fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        // Locale.US here too — hex digits a-f must never be locale-shifted (not a realistic
        // risk for %02x specifically, but consistent + explicit is cheap and removes any doubt).
        return digest.digest().joinToString("") { String.format(Locale.US, "%02x", it) }
    }
}
