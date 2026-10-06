package com.ravalt.app.data.repository

import com.ravalt.app.core.network.ApiClient
import com.ravalt.app.domain.model.BreachResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class BreachRepository(
    private val apiClient: ApiClient
) {

    suspend fun checkPassword(password: String): Result<BreachResult> = withContext(Dispatchers.IO) {
        try {
            if (password.isEmpty()) {
                return@withContext Result.success(BreachResult(false, 0, "", 0))
            }

            // 1. Calculate SHA-1
            val md = MessageDigest.getInstance("SHA-1")
            val hashBytes = md.digest(password.toByteArray(Charsets.UTF_8))
            val fullHashHex = hashBytes.joinToString("") { "%02X".format(it) }

            // 2. Extract 5-char prefix and 35-char suffix
            val prefix = fullHashHex.substring(0, 5)
            val suffix = fullHashHex.substring(5)

            // 3. Query HaveIBeenPwned range API
            val response = apiClient.hibpService.checkHashRange(prefix)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("Gagal menghubungi server HaveIBeenPwned (${response.code()})"))
            }

            val bodyText = response.body()!!.string()
            var breachCount = 0L
            var isBreached = false

            // 4. Match suffix in response lines
            bodyText.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.contains(":")) {
                    val parts = trimmed.split(":")
                    if (parts[0].equals(suffix, ignoreCase = true)) {
                        isBreached = true
                        breachCount = parts[1].toLongOrNull() ?: 1L
                        return@forEach
                    }
                }
            }

            Result.success(
                BreachResult(
                    isBreached = isBreached,
                    count = breachCount,
                    prefix = prefix,
                    passwordLength = password.length
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
