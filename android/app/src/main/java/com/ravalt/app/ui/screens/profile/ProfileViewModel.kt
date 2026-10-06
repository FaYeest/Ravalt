package com.ravalt.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.core.session.SessionManager
import com.ravalt.app.data.local.dao.UserProfileDao
import com.ravalt.app.data.local.entity.UserProfileEntity
import com.ravalt.app.data.repository.AuthRepository
import com.ravalt.app.data.repository.VaultRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val email: String = "",
    val userSalt: String = "",
    val biometricEnabled: Boolean = true,
    val backgroundAuditEnabled: Boolean = true,
    val autoLockSeconds: Int = 0,
    val lastSyncTime: Long = 0L,
    val isSyncing: Boolean = false,
    val isDeleting: Boolean = false,
    val toastMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val userProfileDao: UserProfileDao
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _isDeleting = MutableStateFlow(false)
    private val _isLoggedOut = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)

    val isLoggedOut: StateFlow<Boolean> = _isLoggedOut.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        authRepository.userProfile,
        _isSyncing,
        _isDeleting,
        _toastMessage
    ) { profile, syncing, deleting, toast ->
        ProfileUiState(
            email = profile?.email ?: "farras@ravalt.id",
            userSalt = profile?.userSalt ?: "",
            biometricEnabled = profile?.biometricEnabled ?: true,
            backgroundAuditEnabled = profile?.backgroundAuditEnabled ?: true,
            autoLockSeconds = profile?.autoLockTimeoutSeconds ?: 0,
            lastSyncTime = profile?.lastSyncTime ?: 0L,
            isSyncing = syncing,
            isDeleting = deleting,
            toastMessage = toast
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    fun lockVault() {
        SessionManager.lock()
    }

    fun resetLoggedOutState() {
        _isLoggedOut.value = false
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            val profile = userProfileDao.getProfileSync() ?: return@launch
            userProfileDao.setBiometricEnabled(profile.userId, enabled)
        }
    }

    fun toggleBackgroundAudit(enabled: Boolean) {
        viewModelScope.launch {
            val profile = userProfileDao.getProfileSync() ?: return@launch
            userProfileDao.setBackgroundAuditEnabled(profile.userId, enabled)
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = vaultRepository.syncWithServer()
            _isSyncing.value = false
            res.fold(
                onSuccess = { count ->
                    _toastMessage.value = "Sinkronisasi berhasil ($count item)"
                },
                onFailure = { err ->
                    _toastMessage.value = "Gagal sinkronisasi: ${err.localizedMessage}"
                }
            )
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isDeleting.value = true
            val res = authRepository.deleteAccount()
            _isDeleting.value = false
            res.fold(
                onSuccess = {
                    _isLoggedOut.value = true
                    _toastMessage.value = "Akun dan semua data brankas berhasil dihapus permanen"
                },
                onFailure = { err ->
                    _toastMessage.value = "Gagal menghapus akun: ${err.localizedMessage}"
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedOut.value = true
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
