package com.ravalt.app.ui.screens.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.data.local.entity.UserProfileEntity
import com.ravalt.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UnlockUiState(
    val email: String = "",
    val masterPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUnlocked: Boolean = false,
    val isBiometricAvailable: Boolean = true
)

class UnlockViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.userProfile.collect { profile ->
                if (profile != null) {
                    _uiState.value = _uiState.value.copy(
                        email = profile.email,
                        isBiometricAvailable = profile.biometricEnabled
                    )
                }
            }
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(masterPassword = password, errorMessage = null)
    }

    fun unlock() {
        val state = _uiState.value
        if (state.masterPassword.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Masukkan master password")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = authRepository.unlockWithMasterPassword(state.masterPassword)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isUnlocked = true)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Master password salah"
                    )
                }
            )
        }
    }
}
