package com.ravalt.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Arrays

object SessionManager {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var activeVaultKey: ByteArray? = null
    var activeToken: String? = null
    var activeUserId: String? = null
    var activeEmail: String? = null
    var activeUserSalt: String? = null

    fun setSession(
        userId: String,
        email: String,
        token: String,
        userSalt: String,
        vaultKey: ByteArray
    ) {
        activeUserId = userId
        activeEmail = email
        activeToken = token
        activeUserSalt = userSalt
        activeVaultKey = vaultKey.clone()
        _isUnlocked.value = true
    }

    fun getVaultKey(): ByteArray? {
        return activeVaultKey
    }

    fun lock() {
        activeVaultKey?.let { Arrays.fill(it, 0.toByte()) }
        activeVaultKey = null
        _isUnlocked.value = false
    }

    fun clear() {
        lock()
        activeToken = null
        activeUserId = null
        activeEmail = null
        activeUserSalt = null
    }
}
