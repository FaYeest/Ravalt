package com.ravalt.app.ui.screens.breach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.data.repository.BreachRepository
import com.ravalt.app.domain.model.BreachResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BreachUiState(
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val result: BreachResult? = null,
    val errorMessage: String? = null
)

class BreachViewModel(
    private val breachRepository: BreachRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BreachUiState())
    val uiState: StateFlow<BreachUiState> = _uiState.asStateFlow()

    fun onPasswordInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(
            passwordInput = input,
            result = null,
            errorMessage = null
        )
    }

    fun checkBreach() {
        val state = _uiState.value
        if (state.passwordInput.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Masukkan kata sandi yang ingin diperiksa")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val res = breachRepository.checkPassword(state.passwordInput)
            res.fold(
                onSuccess = { breachRes ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        result = breachRes
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Pemeriksaan gagal"
                    )
                }
            )
        }
    }
}
