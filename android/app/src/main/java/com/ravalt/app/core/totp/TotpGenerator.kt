package com.ravalt.app.core.totp
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class ParsedOtpUri(
    val secret: String,
    val issuer: String?,
    val accountName: String?,
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

object TotpGenerator {

    /**
     * Generates a TOTP code for the given Base32 secret at the given Unix timestamp (default now).
     */
    fun generateCode(
        secretBase32: String,
        timestampSeconds: Long = System.currentTimeMillis() / 1000L,
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String {
        val cleanSecret = secretBase32.replace(" ", "").replace("-", "").uppercase()
        val keyBytes = decodeBase32(cleanSecret)
        if (keyBytes.isEmpty()) return "000000"

        val timeStep = timestampSeconds / period
        val timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array()

        val macAlgorithm = when (algorithm.uppercase()) {
            "SHA256" -> "HmacSHA256"
            "SHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val mac = Mac.getInstance(macAlgorithm)
        mac.init(SecretKeySpec(keyBytes, macAlgorithm))
        val hash = mac.doFinal(timeBytes)

        // Dynamic truncation (RFC 4226 Section 5.4)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val modulo = 10.0.pow(digits.toDouble()).toInt()
        val otp = binary % modulo
        return otp.toString().padStart(digits, '0')
    }

    /**
     * Returns remaining seconds until the current 30s TOTP cycle expires (0 to 30).
     */
    fun getRemainingSeconds(timestampSeconds: Long = System.currentTimeMillis() / 1000L, period: Int = 30): Int {
        return (period - (timestampSeconds % period)).toInt()
    }

    /**
     * Returns progress fraction (1.0f at start down to 0.0f at expiration).
     */
    fun getProgressFraction(timestampSeconds: Long = System.currentTimeMillis() / 1000L, period: Int = 30): Float {
        val remaining = getRemainingSeconds(timestampSeconds, period)
        return remaining.toFloat() / period.toFloat()
    }

    /**
     * Parses standard otpauth://totp/... URI format from QR codes.
     */
    fun parseOtpUri(uriString: String): ParsedOtpUri? {
        if (!uriString.startsWith("otpauth://totp/", ignoreCase = true)) return null

        try {
            val withoutScheme = uriString.substring("otpauth://totp/".length)
            val queryIndex = withoutScheme.indexOf('?')
            val path = if (queryIndex != -1) withoutScheme.substring(0, queryIndex) else withoutScheme
            val queryString = if (queryIndex != -1) withoutScheme.substring(queryIndex + 1) else ""

            val queryParams = queryString.split("&").filter { it.contains("=") }.associate {
                val idx = it.indexOf("=")
                val key = java.net.URLDecoder.decode(it.substring(0, idx), "UTF-8")
                val value = java.net.URLDecoder.decode(it.substring(idx + 1), "UTF-8")
                key to value
            }

            val secret = queryParams["secret"] ?: return null
            val issuerParam = queryParams["issuer"]
            val digits = queryParams["digits"]?.toIntOrNull() ?: 6
            val period = queryParams["period"]?.toIntOrNull() ?: 30
            val algorithm = queryParams["algorithm"] ?: "SHA1"

            val decodedPath = java.net.URLDecoder.decode(path.removePrefix("/"), "UTF-8")
            var accountName: String? = null
            var issuerFromPath: String? = null

            if (decodedPath.contains(":")) {
                val parts = decodedPath.split(":")
                issuerFromPath = parts[0].trim()
                accountName = parts[1].trim()
            } else if (decodedPath.isNotEmpty()) {
                accountName = decodedPath.trim()
            }

            val finalIssuer = issuerParam ?: issuerFromPath

            return ParsedOtpUri(
                secret = secret,
                issuer = finalIssuer,
                accountName = accountName,
                algorithm = algorithm,
                digits = digits,
                period = period
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Standard RFC 4648 Base32 Decoder.
     */
    fun decodeBase32(base32: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = base32.trim().uppercase().replace("=", "")
        if (clean.isEmpty()) return ByteArray(0)

        var buffer = 0
        var bitsLeft = 0
        val output = ArrayList<Byte>()

        for (c in clean) {
            val value = alphabet.indexOf(c)
            if (value < 0) continue // Skip invalid characters

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                output.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }

        return output.toByteArray()
    }
}
