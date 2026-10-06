package com.ravalt.app.core.ssh

import org.junit.Assert.*
import org.junit.Test

class SshKeyGeneratorTest {

    @Test
    fun testGenerateEd25519Key() {
        val keyPair = SshKeyGenerator.generateKeyPair(SshKeyType.ED25519, comment = "test@bastion")

        assertEquals(SshKeyType.ED25519, keyPair.type)
        assertTrue(keyPair.publicKeyOpenSsh.startsWith("ssh-ed25519 "))
        assertTrue(keyPair.publicKeyOpenSsh.endsWith(" test@bastion"))
        assertTrue(keyPair.privateKeyPem.contains("BEGIN OPENSSH PRIVATE KEY"))
        assertTrue(keyPair.fingerprintSha256.startsWith("SHA256:"))
    }
}
