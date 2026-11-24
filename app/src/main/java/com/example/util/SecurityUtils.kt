package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic password hashing, hardware-backed Android Keystore vault encryption,
 * and biometric authentication helpers.
 */
object SecurityUtils {
    private const val HASH_PREFIX = "sha256$"

    fun hashPassword(rawPassword: String): String {
        if (rawPassword.isBlank()) return ""
        if (rawPassword.startsWith(HASH_PREFIX)) return rawPassword
        val saltBytes = ByteArray(12)
        SecureRandom().nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val digest = sha256Hex("$saltHex:$rawPassword")
        return "$HASH_PREFIX$saltHex$$digest"
    }

    fun verifyPassword(rawInput: String, storedValue: String): Boolean {
        if (storedValue.isBlank()) return false
        if (!storedValue.startsWith(HASH_PREFIX)) {
            return rawInput == storedValue
        }
        val body = storedValue.removePrefix(HASH_PREFIX)
        val parts = body.split("$", limit = 2)
        if (parts.size != 2) return false
        val salt = parts[0]
        val expectedHash = parts[1]
        val actualHash = sha256Hex("$salt:$rawInput")
        return MessageDigest.isEqual(actualHash.toByteArray(), expectedHash.toByteArray())
    }

    fun needsHashUpgrade(storedValue: String): Boolean {
        return storedValue.isNotBlank() && !storedValue.startsWith(HASH_PREFIX)
    }

    fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Hardware-backed AES-256-GCM vault backed by AndroidKeyStore (with safe deterministic
 * fallback key derivation for headless JVM unit/Robolectric environments where AndroidKeyStore
 * provider is absent). Never logs or writes plaintext secrets to cloud backup.
 */
object EncryptedKeystoreVault {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "depros_247_vault_aes_key_v1"
    private const val VAULT_PREFS = "depros_secure_vault"
    private const val FALLBACK_SECRET_PREF = "_vault_local_seed_v1"
    private const val CIPHER_PREFIX = "enc_gcm_v1:"
    private const val GCM_TAG_BITS = 128

    private fun getOrCreateAesKey(context: Context): SecretKey {
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER)
            ks.load(null)
            val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return existing

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (_: Exception) {
            // Headless JVM / Robolectric fallback using app-local AES-256 key seeded in private prefs
            val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
            val encodedSeed = prefs.getString(FALLBACK_SECRET_PREF, null)
            val rawKeyBytes = if (!encodedSeed.isNullOrBlank()) {
                Base64.decode(encodedSeed, Base64.NO_WRAP)
            } else {
                ByteArray(32).also {
                    SecureRandom().nextBytes(it)
                    prefs.edit().putString(
                        FALLBACK_SECRET_PREF,
                        Base64.encodeToString(it, Base64.NO_WRAP)
                    ).apply()
                }
            }
            val safeKey = if (rawKeyBytes.size == 32) {
                rawKeyBytes
            } else {
                MessageDigest.getInstance("SHA-256").digest("${context.packageName}:vault_v1".toByteArray())
            }
            return SecretKeySpec(safeKey, "AES")
        }
    }

    fun encrypt(context: Context, plaintext: String): String {
        if (plaintext.isBlank()) return ""
        return try {
            val key = getOrCreateAesKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            CIPHER_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    fun decrypt(context: Context, storedValue: String): String {
        if (storedValue.isBlank()) return ""
        if (!storedValue.startsWith(CIPHER_PREFIX)) {
            return storedValue
        }
        return try {
            val payload = Base64.decode(storedValue.removePrefix(CIPHER_PREFIX), Base64.NO_WRAP)
            if (payload.size <= 12) return ""
            val iv = payload.copyOfRange(0, 12)
            val ciphertext = payload.copyOfRange(12, payload.size)
            val key = getOrCreateAesKey(context)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    fun putSecret(context: Context, key: String, plaintext: String) {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        if (plaintext.isBlank()) {
            prefs.edit().remove(key).apply()
            return
        }
        val encrypted = encrypt(context, plaintext)
        if (encrypted.isNotBlank()) {
            prefs.edit().putString(key, encrypted).apply()
        }
    }

    fun getSecret(context: Context, key: String, fallbackPlaintext: String = ""): String {
        val prefs = context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null)
        if (!raw.isNullOrBlank()) {
            val decrypted = decrypt(context, raw)
            if (decrypted.isNotBlank()) return decrypted
        }
        if (fallbackPlaintext.isNotBlank()) {
            putSecret(context, key, fallbackPlaintext)
            return fallbackPlaintext
        }
        return ""
    }

    fun removeSecret(context: Context, key: String) {
        context.getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE).edit().remove(key).apply()
    }
}

object BiometricLockHelper {
    fun isBiometricAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bm = context.getSystemService(BiometricManager::class.java) ?: return false
            val canAuth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bm.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                bm.canAuthenticate()
            }
            return canAuth == BiometricManager.BIOMETRIC_SUCCESS
        }
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    fun authenticateAdmin(
        context: Context,
        title: String = "Unlock Odiya Control Room",
        subtitle: String = "Verify identity with Fingerprint, Face, or Device PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            onError("Biometric prompt requires Android 9+")
            return
        }
        try {
            val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
            val cancellationSignal = CancellationSignal()
            val builder = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setNegativeButton("Use Password", executor) { _, _ ->
                    onError("Use Admin Security Password below")
                }
            }

            val prompt = builder.build()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                        ) {
                            onError(errString?.toString() ?: "Biometric check failed")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        onError("Fingerprint/Face not recognized. Try again or enter password.")
                    }
                }
            )
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Biometric hardware unavailable")
        }
    }
}
