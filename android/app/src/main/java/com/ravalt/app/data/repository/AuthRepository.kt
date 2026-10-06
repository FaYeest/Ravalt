package com.ravalt.app.data.repository

import com.ravalt.app.core.crypto.CryptoUtils
import com.ravalt.app.core.network.ApiClient
import com.ravalt.app.core.network.dto.LoginRequest
import com.ravalt.app.core.network.dto.RegisterRequest
import com.ravalt.app.core.network.dto.UpdatePasswordRequest
import com.ravalt.app.core.session.SessionManager
import com.ravalt.app.data.local.dao.UserProfileDao
import com.ravalt.app.data.local.dao.VaultDao
import com.ravalt.app.data.local.entity.UserProfileEntity
import com.ravalt.app.data.local.entity.VaultEntity
import com.ravalt.app.domain.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class AuthRepository(
    private val apiClient: ApiClient,
    private val userProfileDao: UserProfileDao,
    private val vaultDao: VaultDao
) {

    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getProfile()

    private suspend fun saveCanary(vaultKey: ByteArray) {
        try {
            val canaryPlaintext = "RAVALT_VERIFIED_KEY".toByteArray(Charsets.UTF_8)
            val encryptedCanary = CryptoUtils.encryptAesGcm(canaryPlaintext, vaultKey)
            vaultDao.insertOrUpdate(
                VaultEntity(
                    id = "__master_canary__",
                    type = "CANARY",
                    encryptedData = encryptedCanary.encryptedDataBase64,
                    nonce = encryptedCanary.nonceBase64,
                    version = 1,
                    updatedAt = System.currentTimeMillis(),
                    isDirty = false,
                    isDeleted = false
                )
            )
        } catch (_: Exception) {}
    }

    suspend fun createLocalVault(masterPassword: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val saltBytes = CryptoUtils.generateSalt(32)
            val saltBase64 = CryptoUtils.toBase64(saltBytes)

            val masterKey = CryptoUtils.deriveMasterKey(masterPassword, saltBytes)
            val keys = CryptoUtils.deriveSubKeys(masterKey)

            saveCanary(keys.vaultKey)

            val localUserId = "local_" + UUID.randomUUID().toString().replace("-", "").take(8)
            val profile = UserProfileEntity(
                userId = localUserId,
                email = "Brankas Lokal (Offline)",
                userSalt = saltBase64,
                authToken = ""
            )
            userProfileDao.clear()
            userProfileDao.insertOrUpdate(profile)

            SessionManager.setSession(
                userId = localUserId,
                email = profile.email,
                token = "",
                userSalt = saltBase64,
                vaultKey = keys.vaultKey
            )

            Result.success(
                UserSession(
                    userId = localUserId,
                    email = profile.email,
                    token = "",
                    userSalt = saltBase64
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, masterPassword: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            // 1. Generate local 32-byte salt
            val saltBytes = CryptoUtils.generateSalt(32)
            val saltBase64 = CryptoUtils.toBase64(saltBytes)

            // 2. Derive MasterKey & SubKeys (AuthHash + VaultKey)
            val masterKey = CryptoUtils.deriveMasterKey(masterPassword, saltBytes)
            val keys = CryptoUtils.deriveSubKeys(masterKey)

            // 3. Register to server
            val response = apiClient.authService.register(
                RegisterRequest(
                    email = cleanEmail,
                    salt = saltBase64,
                    authHash = keys.authHash
                )
            )

            if (!response.isSuccessful || response.body() == null) {
                val errBody = response.errorBody()?.string() ?: "Registrasi gagal (${response.code()})"
                return@withContext Result.failure(Exception(errBody))
            }

            // 4. Auto-login immediately after registration to obtain session token
            val loginResp = apiClient.authService.login(
                LoginRequest(email = cleanEmail, authHash = keys.authHash)
            )

            if (!loginResp.isSuccessful || loginResp.body() == null) {
                val err = loginResp.errorBody()?.string() ?: "Login otomatis gagal (${loginResp.code()})"
                return@withContext Result.failure(Exception(err))
            }

            val authBody = loginResp.body()!!
            val profile = UserProfileEntity(
                userId = authBody.user.id,
                email = cleanEmail,
                userSalt = saltBase64,
                authToken = authBody.token
            )
            userProfileDao.clear()
            userProfileDao.insertOrUpdate(profile)

            SessionManager.setSession(
                userId = authBody.user.id,
                email = cleanEmail,
                token = authBody.token,
                userSalt = saltBase64,
                vaultKey = keys.vaultKey
            )

            saveCanary(keys.vaultKey)

            Result.success(
                UserSession(
                    userId = authBody.user.id,
                    email = cleanEmail,
                    token = authBody.token,
                    userSalt = saltBase64
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, masterPassword: String): Result<UserSession> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()

            // 1. Get Prelogin Salt
            val saltResp = apiClient.authService.getPreloginSalt(cleanEmail)
            if (!saltResp.isSuccessful || saltResp.body() == null) {
                return@withContext Result.failure(Exception("Gagal mengambil salt pre-login (${saltResp.code()})"))
            }

            val saltBase64 = saltResp.body()!!.salt
            val saltBytes = CryptoUtils.fromBase64(saltBase64)

            // 2. Derive MasterKey & SubKeys
            val masterKey = CryptoUtils.deriveMasterKey(masterPassword, saltBytes)
            val keys = CryptoUtils.deriveSubKeys(masterKey)

            // 3. Send AuthHash to server
            val loginResp = apiClient.authService.login(
                LoginRequest(email = cleanEmail, authHash = keys.authHash)
            )

            if (!loginResp.isSuccessful || loginResp.body() == null) {
                val err = if (loginResp.code() == 401) "Email atau master password tidak cocok" else "Login gagal (${loginResp.code()})"
                return@withContext Result.failure(Exception(err))
            }

            val authBody = loginResp.body()!!
            val existing = userProfileDao.getProfileSync()
            val profile = UserProfileEntity(
                userId = authBody.user.id,
                email = cleanEmail,
                userSalt = saltBase64,
                authToken = authBody.token,
                biometricEnabled = existing?.biometricEnabled ?: true,
                autoLockTimeoutSeconds = existing?.autoLockTimeoutSeconds ?: 0,
                backgroundAuditEnabled = existing?.backgroundAuditEnabled ?: true
            )
            userProfileDao.clear()
            userProfileDao.insertOrUpdate(profile)

            SessionManager.setSession(
                userId = authBody.user.id,
                email = cleanEmail,
                token = authBody.token,
                userSalt = saltBase64,
                vaultKey = keys.vaultKey
            )

            saveCanary(keys.vaultKey)

            Result.success(
                UserSession(
                    userId = authBody.user.id,
                    email = cleanEmail,
                    token = authBody.token,
                    userSalt = saltBase64
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockWithMasterPassword(masterPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = userProfileDao.getProfileSync()
                ?: return@withContext Result.failure(Exception("Tidak ada akun tersimpan. Silakan setup brankas terlebih dahulu."))

            val saltBytes = CryptoUtils.fromBase64(profile.userSalt)
            val masterKey = CryptoUtils.deriveMasterKey(masterPassword, saltBytes)
            val keys = CryptoUtils.deriveSubKeys(masterKey)

            // Verify with canary if present
            val canary = vaultDao.getById("__master_canary__")
            if (canary != null) {
                try {
                    val decrypted = CryptoUtils.decryptAesGcm(
                        ciphertextBase64 = canary.encryptedData,
                        nonceBase64 = canary.nonce,
                        key = keys.vaultKey
                    )
                    if (String(decrypted, Charsets.UTF_8) != "RAVALT_VERIFIED_KEY") {
                        return@withContext Result.failure(Exception("Master password salah"))
                    }
                } catch (_: Exception) {
                    return@withContext Result.failure(Exception("Master password salah"))
                }
            }

            SessionManager.setSession(
                userId = profile.userId,
                email = profile.email,
                token = profile.authToken,
                userSalt = profile.userSalt,
                vaultKey = keys.vaultKey
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun linkCloudAccount(email: String, masterPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val profile = userProfileDao.getProfileSync()
                ?: return@withContext Result.failure(Exception("Profil lokal tidak ditemukan"))

            val saltBytes = CryptoUtils.fromBase64(profile.userSalt)
            val masterKey = CryptoUtils.deriveMasterKey(masterPassword, saltBytes)
            val keys = CryptoUtils.deriveSubKeys(masterKey)

            // Try register first in case user doesn't exist yet on server
            apiClient.authService.register(
                RegisterRequest(
                    email = cleanEmail,
                    salt = profile.userSalt,
                    authHash = keys.authHash
                )
            )

            // Login to obtain valid JWT token
            val loginResp = apiClient.authService.login(
                LoginRequest(email = cleanEmail, authHash = keys.authHash)
            )

            if (!loginResp.isSuccessful || loginResp.body() == null) {
                val err = loginResp.errorBody()?.string() ?: "Gagal menghubungkan ke cloud (${loginResp.code()})"
                return@withContext Result.failure(Exception(err))
            }

            val authBody = loginResp.body()!!
            val updatedProfile = profile.copy(
                userId = authBody.user.id,
                email = cleanEmail,
                authToken = authBody.token
            )
            userProfileDao.clear()
            userProfileDao.insertOrUpdate(updatedProfile)

            SessionManager.activeToken = authBody.token
            SessionManager.activeUserId = authBody.user.id
            SessionManager.activeEmail = cleanEmail

            saveCanary(keys.vaultKey)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMasterPassword(newMasterPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profile = userProfileDao.getProfileSync()
                ?: return@withContext Result.failure(Exception("Sesi tidak ditemukan"))

            val newSaltBytes = CryptoUtils.generateSalt(32)
            val newSaltBase64 = CryptoUtils.toBase64(newSaltBytes)

            val newMasterKey = CryptoUtils.deriveMasterKey(newMasterPassword, newSaltBytes)
            val newKeys = CryptoUtils.deriveSubKeys(newMasterKey)

            val response = apiClient.authService.updatePassword(
                UpdatePasswordRequest(
                    newAuthHash = newKeys.authHash,
                    newSalt = newSaltBase64
                )
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal memperbarui master password di server"))
            }

            userProfileDao.insertOrUpdate(
                profile.copy(userSalt = newSaltBase64)
            )

            SessionManager.setSession(
                userId = profile.userId,
                email = profile.email,
                token = profile.authToken,
                userSalt = newSaltBase64,
                vaultKey = newKeys.vaultKey
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.authService.deleteAccount()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gagal menghapus akun di server"))
            }

            vaultDao.clearAll()
            userProfileDao.clear()
            SessionManager.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        SessionManager.clear()
        userProfileDao.clear()
        vaultDao.clearAll()
    }
}
