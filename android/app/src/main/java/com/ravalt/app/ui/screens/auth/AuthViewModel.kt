package com.ravalt.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoginTab: Boolean = true,
    val email: String = "",
    val masterPassword: String = "",
    val confirmPassword: String = "",
    val passwordHint: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun setTab(isLogin: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoginTab = isLogin,
            errorMessage = null
        )
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(masterPassword = password, errorMessage = null)
    }

    fun onConfirmPasswordChanged(confirm: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirm, errorMessage = null)
    }

    fun onPasswordHintChanged(hint: String) {
        _uiState.value = _uiState.value.copy(passwordHint = hint)
    }

    fun submit() {
        val state = _uiState.value
        if (state.email.isBlank() || !state.email.contains("@")) {
            _uiState.value = state.copy(errorMessage = "Format email tidak valid")
            return
        }
        if (state.masterPassword.length < 8) {
            _uiState.value = state.copy(errorMessage = "Master password minimal 8 karakter")
            return
        }

        if (!state.isLoginTab && state.masterPassword != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Konfirmasi password tidak cocok")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            val result = if (state.isLoginTab) {
                authRepository.login(state.email, state.masterPassword)
            } else {
                authRepository.register(state.email, state.masterPassword)
            }

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Terjadi kesalahan autentikasi"
                    )
                }
            )
        }
    }
}
