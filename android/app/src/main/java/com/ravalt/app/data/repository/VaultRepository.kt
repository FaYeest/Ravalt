package com.ravalt.app.data.repository

import com.ravalt.app.core.crypto.CryptoUtils
import com.ravalt.app.core.network.ApiClient
import com.ravalt.app.core.network.dto.SyncItemDto
import com.ravalt.app.core.network.dto.SyncRequest
import com.ravalt.app.core.session.SessionManager
import com.ravalt.app.data.local.dao.UserProfileDao
import com.ravalt.app.data.local.dao.VaultDao
import com.ravalt.app.data.local.entity.VaultEntity
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.domain.model.VaultPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter

class VaultRepository(
    private val apiClient: ApiClient,
    private val vaultDao: VaultDao,
    private val userProfileDao: UserProfileDao
) {

    /**
     * Emits a reactive flow of decrypted vault items.
     */
    val vaultItems: Flow<List<VaultItem>> = vaultDao.getAllActive().map { entities ->
        val key = SessionManager.getVaultKey() ?: return@map emptyList()
        entities.mapNotNull { entity ->
            try {
                val decryptedBytes = CryptoUtils.decryptAesGcm(
                    ciphertextBase64 = entity.encryptedData,
                    nonceBase64 = entity.nonce,
                    key = key
                )
                val json = String(decryptedBytes, Charsets.UTF_8)
                val payload = VaultPayload.fromJson(json)
                VaultItem.fromPayload(
                    id = entity.id,
                    payload = payload,
                    version = entity.version,
                    updatedAt = entity.updatedAt
                ).copy(isDirty = entity.isDirty)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun saveItem(item: VaultItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val key = SessionManager.getVaultKey()
                ?: return@withContext Result.failure(Exception("Brankas terkunci"))

            val payloadJson = item.toPayload().toJson()
            val encrypted = CryptoUtils.encryptAesGcm(
                plaintext = payloadJson.toByteArray(Charsets.UTF_8),
                key = key
            )

            val entity = VaultEntity(
                id = item.id,
                type = item.type.name,
                encryptedData = encrypted.encryptedDataBase64,
                nonce = encrypted.nonceBase64,
                version = item.version + 1,
                updatedAt = System.currentTimeMillis(),
                isDirty = true,
                isDeleted = false
            )

            vaultDao.insertOrUpdate(entity)

            // Attempt silent background sync
            syncWithServer()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            vaultDao.markDeleted(id)
            syncWithServer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncWithServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val token = SessionManager.activeToken
            if (token.isNullOrBlank()) {
                return@withContext Result.success(0)
            }

            val profile = userProfileDao.getProfileSync()
            val sinceIso = if (profile != null && profile.lastSyncTime > 0) {
                DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(profile.lastSyncTime))
            } else {
                null
            }

            val dirtyEntities = vaultDao.getDirtyItems()
            val deletedIds = vaultDao.getDeletedIds()

            val syncItems = dirtyEntities.map { entity ->
                SyncItemDto(
                    id = entity.id,
                    encryptedData = entity.encryptedData,
                    nonce = entity.nonce,
                    version = entity.version
                )
            }

            val request = SyncRequest(
                since = sinceIso,
                items = syncItems,
                deletedIds = deletedIds
            )

            val response = apiClient.vaultService.syncVault(request)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("Sinkronisasi gagal (${response.code()})"))
            }

            val syncBody = response.body()!!

            // 1. Mark dirty items as clean
            dirtyEntities.forEach { entity ->
                vaultDao.update(entity.copy(isDirty = false))
            }

            // 2. Permanently delete acknowledged deletions
            deletedIds.forEach { delId ->
                vaultDao.deletePermanently(delId)
            }

            // 3. Upsert items returned by server
            val serverEntities = syncBody.serverItems.map { dto ->
                VaultEntity(
                    id = dto.id,
                    type = "LOGIN", // will be determined by decrypted payload
                    encryptedData = dto.encryptedData,
                    nonce = dto.nonce,
                    version = dto.version,
                    updatedAt = System.currentTimeMillis(),
                    isDirty = false,
                    isDeleted = false
                )
            }
            vaultDao.insertOrUpdateAll(serverEntities)

            // 4. Update lastSyncTime
            val now = System.currentTimeMillis()
            if (profile != null) {
                userProfileDao.updateLastSyncTime(profile.userId, now)
            }

            Result.success(serverEntities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
