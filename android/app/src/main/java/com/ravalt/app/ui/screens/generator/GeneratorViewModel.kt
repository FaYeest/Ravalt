package com.ravalt.app.ui.screens.generator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import kotlin.math.log2

data class GeneratorUiState(
    val length: Int = 20,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val generatedPassword: String = "",
    val entropyBits: Int = 0,
    val strengthLabel: String = "Sangat Kuat"
)

class GeneratorViewModel : ViewModel() {

    private val random = SecureRandom()
    private val _uiState = MutableStateFlow(GeneratorUiState())
    val uiState: StateFlow<GeneratorUiState> = _uiState.asStateFlow()

    init {
        regenerate()
    }

    fun setLength(newLength: Int) {
        _uiState.value = _uiState.value.copy(length = newLength)
        regenerate()
    }

    fun toggleUppercase(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeLowercase && !_uiState.value.includeNumbers && !_uiState.value.includeSymbols) return
        _uiState.value = _uiState.value.copy(includeUppercase = enabled)
        regenerate()
    }

    fun toggleLowercase(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeUppercase && !_uiState.value.includeNumbers && !_uiState.value.includeSymbols) return
        _uiState.value = _uiState.value.copy(includeLowercase = enabled)
        regenerate()
    }

    fun toggleNumbers(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeUppercase && !_uiState.value.includeLowercase && !_uiState.value.includeSymbols) return
        _uiState.value = _uiState.value.copy(includeNumbers = enabled)
        regenerate()
    }

    fun toggleSymbols(enabled: Boolean) {
        if (!enabled && !_uiState.value.includeUppercase && !_uiState.value.includeLowercase && !_uiState.value.includeNumbers) return
        _uiState.value = _uiState.value.copy(includeSymbols = enabled)
        regenerate()
    }

    fun regenerate() {
        val state = _uiState.value
        val pool = StringBuilder()
        var poolSize = 0

        if (state.includeUppercase) { pool.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ"); poolSize += 26 }
        if (state.includeLowercase) { pool.append("abcdefghijklmnopqrstuvwxyz"); poolSize += 26 }
        if (state.includeNumbers) { pool.append("0123456789"); poolSize += 10 }
        if (state.includeSymbols) { pool.append("!@#$%^&*()_+-=[]{}|;:,.<>?"); poolSize += 26 }

        if (pool.isEmpty()) return

        val chars = pool.toString()
        val pwd = (1..state.length).map { chars[random.nextInt(chars.length)] }.joinToString("")
        val entropy = if (poolSize > 0) (state.length * log2(poolSize.toDouble())).toInt() else 0

        val label = when {
            entropy < 50 -> "Lemah"
            entropy < 75 -> "Cukup"
            entropy < 100 -> "Kuat"
            else -> "Sangat Kuat"
        }

        _uiState.value = state.copy(
            generatedPassword = pwd,
            entropyBits = entropy,
            strengthLabel = label
        )
    }
}
