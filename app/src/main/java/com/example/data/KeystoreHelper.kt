package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

object KeystoreHelper {
    private const val KEY_ALIAS = "PawsSqlCipherKeyAlias"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PREFS_NAME = "paws_security_prefs"
    private const val ENCRYPTED_KEY_PREF = "encrypted_db_passphrase"
    private const val IV_PREF = "db_passphrase_iv"
    private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

    @Synchronized
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(ENCRYPTED_KEY_PREF, null)
        val ivBase64 = prefs.getString(IV_PREF, null)

        return if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
                val ivBytes = Base64.decode(ivBase64, Base64.DEFAULT)
                decryptPassphrase(encryptedBytes, ivBytes)
            } catch (e: Exception) {
                // If decryption fails (e.g. key invalidated), generate a new one
                e.printStackTrace()
                generateAndStoreNewPassphrase(context)
            }
        } else {
            generateAndStoreNewPassphrase(context)
        }
    }

    private fun generateAndStoreNewPassphrase(context: Context): ByteArray {
        // Generate a random 32-byte passphrase
        val rawPassphrase = ByteArray(32)
        SecureRandom().nextBytes(rawPassphrase)

        // Get or generate the Keystore key
        val secretKey = getOrCreateKeystoreKey()

        // Encrypt the passphrase
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(rawPassphrase)
        val iv = cipher.iv

        // Store encrypted passphrase and IV in SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(ENCRYPTED_KEY_PREF, Base64.encodeToString(encryptedBytes, Base64.DEFAULT))
            .putString(IV_PREF, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()

        return rawPassphrase
    }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        // Generate a new AES key in Android Keystore
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    private fun decryptPassphrase(encryptedBytes: ByteArray, ivBytes: ByteArray): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val secretKey = (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ivBytes))
        return cipher.doFinal(encryptedBytes)
    }
}
