package com.ravalt.app.core.ssh

import java.util.Base64
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey

enum class SshKeyType {
    ED25519,
    RSA_4096
}

data class GeneratedSshKeyPair(
    val type: SshKeyType,
    val publicKeyOpenSsh: String,
    val privateKeyPem: String,
    val fingerprintSha256: String
)

object SshKeyGenerator {

    private val secureRandom = SecureRandom()

    fun generateKeyPair(type: SshKeyType, comment: String = "ravalt-vault"): GeneratedSshKeyPair {
        return when (type) {
            SshKeyType.ED25519 -> generateEd25519(comment)
            SshKeyType.RSA_4096 -> generateRsa(4096, comment)
        }
    }

    private fun generateEd25519(comment: String): GeneratedSshKeyPair {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(secureRandom))
        val keyPair = generator.generateKeyPair()

        val pub = keyPair.public as Ed25519PublicKeyParameters
        val priv = keyPair.private as Ed25519PrivateKeyParameters

        val pubBytes = pub.encoded
        val privBytes = priv.encoded

        // OpenSSH wire format for ed25519:
        // string "ssh-ed25519"
        // string <pubkey-bytes>
        val pubWire = encodeSshWire("ssh-ed25519", pubBytes)
        val pubBase64 = Base64.getEncoder().encodeToString(pubWire)
        val pubOpenSsh = "ssh-ed25519 $pubBase64 $comment"

        val privBase64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(privBytes)
        val privPem = "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                privBase64.trim() +
                "\n-----END OPENSSH PRIVATE KEY-----"

        val fingerprint = calculateFingerprint(pubWire)

        return GeneratedSshKeyPair(
            type = SshKeyType.ED25519,
            publicKeyOpenSsh = pubOpenSsh,
            privateKeyPem = privPem,
            fingerprintSha256 = fingerprint
        )
    }

    private fun generateRsa(keySize: Int, comment: String): GeneratedSshKeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(keySize, secureRandom)
        val kp = kpg.generateKeyPair()

        val rsaPub = kp.public as RSAPublicKey
        val rsaPriv = kp.private as RSAPrivateCrtKey

        // OpenSSH wire format for rsa:
        // string "ssh-rsa"
        // mpint exponent
        // mpint modulus
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        writeSshString(dos, "ssh-rsa".toByteArray(Charsets.UTF_8))
        writeSshMpint(dos, rsaPub.publicExponent.toByteArray())
        writeSshMpint(dos, rsaPub.modulus.toByteArray())
        dos.flush()

        val pubWire = baos.toByteArray()
        val pubBase64 = Base64.getEncoder().encodeToString(pubWire)
        val pubOpenSsh = "ssh-rsa $pubBase64 $comment"

        val privBase64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(rsaPriv.encoded)
        val privPem = "-----BEGIN RSA PRIVATE KEY-----\n" +
                privBase64.trim() +
                "\n-----END RSA PRIVATE KEY-----"

        val fingerprint = calculateFingerprint(pubWire)

        return GeneratedSshKeyPair(
            type = SshKeyType.RSA_4096,
            publicKeyOpenSsh = pubOpenSsh,
            privateKeyPem = privPem,
            fingerprintSha256 = fingerprint
        )
    }

    fun calculateFingerprint(publicKeyWireBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(publicKeyWireBytes)
        val b64 = Base64.getEncoder().encodeToString(digest).removeSuffix("=")
        return "SHA256:$b64"
    }

    private fun encodeSshWire(keyType: String, keyBytes: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)
        writeSshString(dos, keyType.toByteArray(Charsets.UTF_8))
        writeSshString(dos, keyBytes)
        dos.flush()
        return baos.toByteArray()
    }

    private fun writeSshString(dos: DataOutputStream, data: ByteArray) {
        dos.writeInt(data.size)
        dos.write(data)
    }

    private fun writeSshMpint(dos: DataOutputStream, bytes: ByteArray) {
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }
}
