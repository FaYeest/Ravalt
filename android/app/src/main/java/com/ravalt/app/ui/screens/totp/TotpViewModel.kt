package com.ravalt.app.ui.screens.totp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.core.totp.TotpGenerator
import com.ravalt.app.data.repository.VaultRepository
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.domain.model.VaultItemType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ActiveTotpEntry(
    val item: VaultItem,
    val code: String,
    val remainingSeconds: Int,
    val progress: Float
)

data class TotpUiState(
    val entries: List<ActiveTotpEntry> = emptyList(),
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val toastMessage: String? = null
)

class TotpViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showAddDialog = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)
    private val _currentTicker = MutableStateFlow(System.currentTimeMillis() / 1000L)

    init {
        viewModelScope.launch {
            while (isActive) {
                _currentTicker.value = System.currentTimeMillis() / 1000L
                delay(1000L)
            }
        }
    }

    val uiState: StateFlow<TotpUiState> = combine(
        vaultRepository.vaultItems,
        _searchQuery,
        _currentTicker,
        _showAddDialog,
        _toastMessage
    ) { items, query, timestamp, showAdd, toast ->
        val totpItems = items.filter { it.type == VaultItemType.TOTP }

        val entries = totpItems.filter { item ->
            query.isBlank() ||
                    item.totpIssuer.contains(query, ignoreCase = true) ||
                    item.totpAccount.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true)
        }.map { item ->
            val code = TotpGenerator.generateCode(
                secretBase32 = item.totpSecret,
                timestampSeconds = timestamp,
                period = item.totpPeriod,
                digits = item.totpDigits
            )
            val remaining = TotpGenerator.getRemainingSeconds(timestamp, item.totpPeriod)
            val progress = TotpGenerator.getProgressFraction(timestamp, item.totpPeriod)

            ActiveTotpEntry(
                item = item,
                code = code,
                remainingSeconds = remaining,
                progress = progress
            )
        }

        TotpUiState(
            entries = entries,
            searchQuery = query,
            showAddDialog = showAdd,
            toastMessage = toast
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TotpUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
    }

    fun addTotp(issuer: String, account: String, secret: String) {
        viewModelScope.launch {
            val cleanSecret = secret.replace(" ", "").trim()
            val newItem = VaultItem(
                title = issuer.ifBlank { "2FA Token" },
                type = VaultItemType.TOTP,
                totpIssuer = issuer.trim(),
                totpAccount = account.trim(),
                totpSecret = cleanSecret
            )
            vaultRepository.saveItem(newItem)
            _showAddDialog.value = false
            _toastMessage.value = "Akun 2FA berhasil ditambahkan"
        }
    }

    fun deleteTotp(id: String) {
        viewModelScope.launch {
            vaultRepository.deleteItem(id)
            _toastMessage.value = "Token 2FA dihapus"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
