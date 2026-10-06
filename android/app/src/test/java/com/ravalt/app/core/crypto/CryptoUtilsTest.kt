package com.ravalt.app.core.crypto

import org.junit.Assert.*
import org.junit.Test

class CryptoUtilsTest {

    @Test
    fun testGenerateSalt_length() {
        val salt = CryptoUtils.generateSalt(32)
        assertEquals(32, salt.size)
    }

    @Test
    fun testDeriveMasterKey_deterministic() {
        val salt = "01234567890123456789012345678901".toByteArray()
        val pass = "SuperSecretMasterPassword123!"

        val key1 = CryptoUtils.deriveMasterKey(pass, salt)
        val key2 = CryptoUtils.deriveMasterKey(pass, salt)

        assertEquals(32, key1.size)
        assertArrayEquals(key1, key2)
    }

    @Test
    fun testDeriveSubKeys_separation() {
        val masterKey = ByteArray(32) { it.toByte() }
        val keys = CryptoUtils.deriveSubKeys(masterKey)

        assertNotNull(keys.authHash)
        assertTrue(keys.authHash.isNotBlank())
        assertEquals(32, keys.vaultKey.size)

        // Ensure authHash bytes are not identical to vaultKey bytes
        val authBytes = CryptoUtils.fromBase64(keys.authHash)
        assertFalse(authBytes.contentEquals(keys.vaultKey))
    }

    @Test
    fun testAesGcm_encryptionDecryption() {
        val vaultKey = ByteArray(32) { (it * 3).toByte() }
        val originalText = "Hello, Ravalt Zero-Knowledge Security!"

        val encrypted = CryptoUtils.encryptAesGcm(
            plaintext = originalText.toByteArray(Charsets.UTF_8),
            key = vaultKey
        )

        assertNotNull(encrypted.encryptedDataBase64)
        assertNotNull(encrypted.nonceBase64)

        val decryptedBytes = CryptoUtils.decryptAesGcm(
            ciphertextBase64 = encrypted.encryptedDataBase64,
            nonceBase64 = encrypted.nonceBase64,
            key = vaultKey
        )

        val decryptedText = String(decryptedBytes, Charsets.UTF_8)
        assertEquals(originalText, decryptedText)
    }
}
