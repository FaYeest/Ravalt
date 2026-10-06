package com.ravalt.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ravalt.app.data.local.entity.UserProfileEntity
import com.ravalt.app.data.local.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_items WHERE isDeleted = 0 AND id != '__master_canary__' ORDER BY updatedAt DESC")
    fun getAllActive(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_items WHERE isDirty = 1 AND id != '__master_canary__'")
    suspend fun getDirtyItems(): List<VaultEntity>

    @Query("SELECT id FROM vault_items WHERE isDeleted = 1 AND id != '__master_canary__'")
    suspend fun getDeletedIds(): List<String>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: VaultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(items: List<VaultEntity>)

    @Update
    suspend fun update(item: VaultEntity)

    @Query("UPDATE vault_items SET isDeleted = 1, isDirty = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun markDeleted(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM vault_items")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET biometricEnabled = :enabled WHERE userId = :userId")
    suspend fun setBiometricEnabled(userId: String, enabled: Boolean)

    @Query("UPDATE user_profiles SET backgroundAuditEnabled = :enabled WHERE userId = :userId")
    suspend fun setBackgroundAuditEnabled(userId: String, enabled: Boolean)

    @Query("UPDATE user_profiles SET autoLockTimeoutSeconds = :seconds WHERE userId = :userId")
    suspend fun setAutoLockTimeout(userId: String, seconds: Int)

    @Query("UPDATE user_profiles SET lastSyncTime = :syncTime WHERE userId = :userId")
    suspend fun updateLastSyncTime(userId: String, syncTime: Long)

    @Query("DELETE FROM user_profiles")
    suspend fun clear()
}
