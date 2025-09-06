package com.example.antibully.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec



object Encryption {
    private const val SECRET_KEY = "47318c6256e5cf68f9a6e6bce9a04d459bee29226a3cd9a981526f1b2ba35561"

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }

    fun encrypt(plainText: String): String {
        val keyBytes = SECRET_KEY.padEnd(32, '0').substring(0, 32).toByteArray(Charsets.UTF_8)
        val keySpec = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ivHex = iv.joinToString("") { "%02x".format(it) }
        val encryptedHex = encrypted.joinToString("") { "%02x".format(it) }

        return "$ivHex:$encryptedHex"
    }

}

