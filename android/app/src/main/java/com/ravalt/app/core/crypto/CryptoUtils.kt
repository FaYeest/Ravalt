package com.ravalt.app.core.crypto

import java.util.Base64
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.HKDFParameters
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class CryptoKeys(
    val masterKey: ByteArray,
    val authHash: String,       // Base64 string sent to server
    val vaultKey: ByteArray      // Kept in memory/Keystore, NEVER sent to server
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CryptoKeys
        return masterKey.contentEquals(other.masterKey) &&
                authHash == other.authHash &&
                vaultKey.contentEquals(other.vaultKey)
    }

    override fun hashCode(): Int {
        var result = masterKey.contentHashCode()
        result = 31 * result + authHash.hashCode()
        result = 31 * result + vaultKey.contentHashCode()
        return result
    }
}

data class EncryptedPayload(
    val encryptedDataBase64: String,
    val nonceBase64: String
)

object CryptoUtils {
    private val secureRandom = SecureRandom()
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_NONCE_LENGTH_BYTES = 12

    fun generateSalt(length: Int = 32): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun toBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun fromBase64(base64: String): ByteArray {
        return Base64.getDecoder().decode(base64.trim())
    }

    /**
     * Derives a 32-byte MasterKey from master password and user salt using Argon2id.
     * Argon2id Parameters: memory = 64MB (65536 KiB), iterations = 3, parallelism = 4.
     */
    fun deriveMasterKey(password: String, salt: ByteArray): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3)
            .withMemoryAsKB(65536)
            .withParallelism(4)
            .withSalt(salt)

        val generate = Argon2BytesGenerator()
        generate.init(builder.build())

        val masterKey = ByteArray(32)
        generate.generateBytes(password.toByteArray(Charsets.UTF_8), masterKey)
        return masterKey
    }

    /**
     * Derives separate authHash (for server authentication) and vaultKey (for local AES-GCM)
     * using HKDF-SHA256 as specified in the Zero-Knowledge protocol.
     */
    fun deriveSubKeys(masterKey: ByteArray): CryptoKeys {
        val authKeyBytes = hkdfExpand(masterKey, "ravalt-auth-v1".toByteArray(Charsets.UTF_8), 32)
        val vaultKeyBytes = hkdfExpand(masterKey, "ravalt-vault-v1".toByteArray(Charsets.UTF_8), 32)

        return CryptoKeys(
            masterKey = masterKey,
            authHash = toBase64(authKeyBytes),
            vaultKey = vaultKeyBytes
        )
    }

    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hkdf = HKDFBytesGenerator(SHA256Digest())
        hkdf.init(HKDFParameters(prk, null, info))
        val okm = ByteArray(length)
        hkdf.generateBytes(okm, 0, length)
        return okm
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a fresh 12-byte random nonce.
     */
    fun encryptAesGcm(plaintext: ByteArray, key: ByteArray): EncryptedPayload {
        val nonce = ByteArray(GCM_NONCE_LENGTH_BYTES)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptedPayload(
            encryptedDataBase64 = toBase64(ciphertext),
            nonceBase64 = toBase64(nonce)
        )
    }

    /**
     * Decrypts AES-256-GCM ciphertext using the given nonce and key.
     */
    fun decryptAesGcm(ciphertextBase64: String, nonceBase64: String, key: ByteArray): ByteArray {
        val ciphertext = fromBase64(ciphertextBase64)
        val nonce = fromBase64(nonceBase64)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(ciphertext)
    }
}
