package com.example.data

import android.util.Base64
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object BCryptHelper {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Hashing a plaintext password using PBKDF2.
     * Returns a string in format "iterations:saltBase64:hashBase64"
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)
        
        return "$ITERATIONS:$saltBase64:$hashBase64"
    }

    fun isHashedPassword(value: String): Boolean {
        val parts = value.split(":")
        return parts.size == 3 && parts[0].toIntOrNull() != null && parts[1].isNotBlank() && parts[2].isNotBlank()
    }

    /**
     * Verifies if a plaintext password matches the stored hash.
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (!isHashedPassword(storedHash)) return false
        val parts = storedHash.split(":")
        
        return try {
            val iterations = parts[0].toInt()
            val salt = Base64.decode(parts[1], Base64.DEFAULT)
            val hash = Base64.decode(parts[2], Base64.DEFAULT)
            
            val testHash = pbkdf2(password.toCharArray(), salt, iterations, KEY_LENGTH)
            
            // Constant-time comparison to prevent timing attacks
            var diff = hash.size xor testHash.size
            for (i in 0 until minOf(hash.size, testHash.size)) {
                diff = diff or (hash[i].toInt() xor testHash[i].toInt())
            }
            diff == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        return try {
            val skf = SecretKeyFactory.getInstance(ALGORITHM)
            skf.generateSecret(spec).encoded
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Hash algorithm not found: $ALGORITHM", e)
        } catch (e: InvalidKeySpecException) {
            throw RuntimeException("Invalid key specification for hashing", e)
        } finally {
            spec.clearPassword()
        }
    }
}
