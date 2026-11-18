package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Cryptographic password hashing & biometric authentication helpers.
 * Uses salted SHA-256 with backward-compatible verification so legacy plain credentials
 * seamlessly upgrade to salted hashes on sign-in.
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

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
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
        title: String = "Unlock 24/7 Shopping Control Room",
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
