package com.vvf.smartmanager.core.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and verifies the Vault PIN. The PIN itself is never stored — only a salted
 * PBKDF2 hash — so a leaked preferences file cannot be reversed to the PIN directly.
 * This gates access to the Vault UI; the actual file encryption key is a separate
 * random Keystore-wrapped value (see [VaultCryptoManager]), so a weak/guessed PIN
 * does not by itself expose already-encrypted vault file contents.
 *
 * Includes a failed-attempt lockout (exponential backoff, capped) since without one,
 * an attacker with app-level code execution could brute-force PIN attempts indefinitely.
 */
@Singleton
class VaultPinManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val masterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "vvf_vault_pin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun hasPinConfigured(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    /**
     * @throws VaultLockedOutException if a previous run of failed attempts has triggered a
     * temporary lockout that hasn't expired yet.
     */
    fun verifyPin(pin: String): Boolean {
        val now = System.currentTimeMillis()
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        if (now < lockoutUntil) {
            val remainingSeconds = (lockoutUntil - now) / 1000 + 1
            throw VaultLockedOutException(remainingSeconds)
        }

        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val expectedHashB64 = prefs.getString(KEY_HASH, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val computed = pbkdf2(pin, salt)
        val expected = Base64.decode(expectedHashB64, Base64.NO_WRAP)

        // MessageDigest.isEqual is constant-time regardless of where the first mismatching
        // byte occurs — plain ByteArray.contentEquals() short-circuits and can theoretically
        // leak timing information about how many leading bytes matched.
        val matches = MessageDigest.isEqual(computed, expected)

        if (matches) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0L).apply()
        } else {
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
                val lockoutTier = attempts - MAX_ATTEMPTS_BEFORE_LOCKOUT
                val lockoutDurationMs = (BASE_LOCKOUT_MS shl lockoutTier.coerceAtMost(10))
                    .coerceAtMost(MAX_LOCKOUT_MS)
                editor.putLong(KEY_LOCKOUT_UNTIL, now + lockoutDurationMs)
            }
            editor.apply()
        }
        return matches
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private companion object {
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
        const val KEY_LOCKOUT_UNTIL = "pin_lockout_until"
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
        const val MAX_ATTEMPTS_BEFORE_LOCKOUT = 5
        const val BASE_LOCKOUT_MS = 30_000L // 30 seconds after the 5th failure
        const val MAX_LOCKOUT_MS = 30 * 60_000L // capped at 30 minutes
    }
}

/** Thrown by [VaultPinManager.verifyPin] when a failed-attempt lockout is currently active. */
class VaultLockedOutException(val remainingSeconds: Long) :
    Exception("बहुत सारे गलत PIN प्रयास — $remainingSeconds सेकंड बाद फिर कोशिश करें")
