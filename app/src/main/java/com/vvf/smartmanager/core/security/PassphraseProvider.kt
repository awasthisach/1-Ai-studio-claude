package com.vvf.smartmanager.core.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the passphrase used to open the SQLCipher-encrypted Room database.
 *
 * The passphrase itself is a random 256-bit value generated once on first launch.
 * It is stored in [EncryptedSharedPreferences], which wraps it with a key that lives
 * in the Android Keystore (StrongBox/TEE where available) — the passphrase is never
 * hardcoded and never leaves the device.
 *
 * Master Specification v2.0, Section 7 (Security Objectives): AES-256-GCM, Android Keystore,
 * sensitive data never stored in plain text.
 */
@Singleton
class PassphraseProvider @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val prefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Returns the database passphrase as a [CharArray], generating and persisting a new
     * random one on first call. SQLCipher's SupportFactory consumes this directly; callers
     * must clear/overwrite the array after use where possible.
     */
    fun getOrCreateDatabasePassphrase(): CharArray {
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) {
            return existing.toCharArray()
        }
        val newPassphrase = generateRandomPassphrase()
        // commit() (not apply()) deliberately, since this is a one-time, non-hot-path write:
        // if the process died between an async apply() and its eventual flush, the app would
        // believe it had persisted a passphrase it actually hadn't, corrupting DB access on
        // next launch. The synchronous write here is the correctness fix, not just style.
        val persisted = prefs.edit().putString(KEY_DB_PASSPHRASE, newPassphrase).commit()
        check(persisted) { "Database passphrase को secure storage में save नहीं किया जा सका" }
        return newPassphrase.toCharArray()
    }

    /**
     * Returns (creating if necessary) a random 256-bit key used to wrap individual
     * Vault file contents (AES-256-GCM, per-file random IV — see [VaultCryptoManager]).
     */
    fun getOrCreateVaultKeyBase64(): String {
        val existing = prefs.getString(KEY_VAULT_KEY, null)
        if (existing != null) return existing
        val raw = ByteArray(32)
        SecureRandom().nextBytes(raw)
        val encoded = Base64.encodeToString(raw, Base64.NO_WRAP)
        val persisted = prefs.edit().putString(KEY_VAULT_KEY, encoded).commit()
        check(persisted) { "Vault key को secure storage में save नहीं किया जा सका" }
        return encoded
    }

    private fun generateRandomPassphrase(): String {
        val raw = ByteArray(32)
        SecureRandom().nextBytes(raw)
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    private companion object {
        const val PREFS_FILE_NAME = "vvf_secure_prefs"
        const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
        const val KEY_VAULT_KEY = "vault_key_v1"
    }
}
