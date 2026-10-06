package com.ravalt.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey val id: String,
    val type: String,
    val encryptedData: String,
    val nonce: String,
    val version: Int,
    val updatedAt: Long,
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val userSalt: String,
    val authToken: String,
    val lastSyncTime: Long = 0L,
    val biometricEnabled: Boolean = true,
    val autoLockTimeoutSeconds: Int = 0, // 0 = immediately, 60 = 1m, 300 = 5m
    val backgroundAuditEnabled: Boolean = true
)
