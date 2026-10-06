package com.ravalt.app.core.totp

import org.junit.Assert.*
import org.junit.Test

class TotpGeneratorTest {

    @Test
    fun testBase32Decoder() {
        val decoded = TotpGenerator.decodeBase32("JBSWY3DPEE")
        val text = String(decoded, Charsets.UTF_8)
        assertEquals("Hello!", text)
    }

    @Test
    fun testTotpGeneration_rfcTestVector() {
        // RFC 6238 seed "12345678901234567890" Base32: GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ
        val secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        // At timestamp 59, timeStep = 59 / 30 = 1 -> code 287082
        val code59 = TotpGenerator.generateCode(secret, timestampSeconds = 59L)
        assertEquals("287082", code59)

        // At timestamp 1111111109, timeStep = 37037036 -> code 081804
        val code1111111109 = TotpGenerator.generateCode(secret, timestampSeconds = 1111111109L)
        assertEquals("081804", code1111111109)
    }

    @Test
    fun testParseOtpUri() {
        val uri = "otpauth://totp/GitHub:user123?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&digits=6&period=30"
        val parsed = TotpGenerator.parseOtpUri(uri)

        assertNotNull(parsed)
        assertEquals("JBSWY3DPEHPK3PXP", parsed?.secret)
        assertEquals("GitHub", parsed?.issuer)
        assertEquals("user123", parsed?.accountName)
        assertEquals(6, parsed?.digits)
        assertEquals(30, parsed?.period)
    }
}
