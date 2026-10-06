package com.ravalt.app.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.data.repository.VaultRepository
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.domain.model.VaultItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultFilterTab {
    ALL,
    LOGINS,
    NOTES,
    CARDS
}

data class VaultUiState(
    val items: List<VaultItem> = emptyList(),
    val filteredItems: List<VaultItem> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: VaultFilterTab = VaultFilterTab.ALL,
    val isSyncing: Boolean = false,
    val showAddDialog: Boolean = false,
    val message: String? = null
)

class VaultViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTab = MutableStateFlow(VaultFilterTab.ALL)
    private val _isSyncing = MutableStateFlow(false)
    private val _showAddDialog = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VaultUiState> = combine(
        vaultRepository.vaultItems,
        _searchQuery,
        _selectedTab,
        _isSyncing,
        combine(_showAddDialog, _message) { showAdd, msg -> showAdd to msg }
    ) { items, query, tab, syncing, dialogAndMsg ->
        val (showAdd, msg) = dialogAndMsg
        val filtered = items.filter { item ->
            // Filter out SSH and TOTP from main vault list (they have their own tabs)
            val isMainVaultItem = item.type == VaultItemType.LOGIN ||
                    item.type == VaultItemType.NOTE ||
                    item.type == VaultItemType.CARD

            if (!isMainVaultItem) return@filter false

            val matchesTab = when (tab) {
                VaultFilterTab.ALL -> true
                VaultFilterTab.LOGINS -> item.type == VaultItemType.LOGIN
                VaultFilterTab.NOTES -> item.type == VaultItemType.NOTE
                VaultFilterTab.CARDS -> item.type == VaultItemType.CARD
            }

            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.username.contains(query, ignoreCase = true) ||
                    item.url.contains(query, ignoreCase = true)

            matchesTab && matchesQuery
        }

        VaultUiState(
            items = items,
            filteredItems = filtered,
            searchQuery = query,
            selectedTab = tab,
            isSyncing = syncing,
            showAddDialog = showAdd,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(tab: VaultFilterTab) {
        _selectedTab.value = tab
    }

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
    }

    fun saveItem(item: VaultItem) {
        viewModelScope.launch {
            val res = vaultRepository.saveItem(item)
            res.fold(
                onSuccess = {
                    _showAddDialog.value = false
                    _message.value = "Item tersimpan aman di brankas"
                },
                onFailure = { err ->
                    _message.value = err.localizedMessage ?: "Gagal menyimpan item"
                }
            )
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            vaultRepository.deleteItem(id)
            _message.value = "Item dihapus dari brankas"
        }
    }

    fun sync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = vaultRepository.syncWithServer()
            _isSyncing.value = false
            res.fold(
                onSuccess = { count ->
                    _message.value = "Sinkronisasi selesai ($count item diperbarui)"
                },
                onFailure = { err ->
                    _message.value = "Sinkronisasi gagal: ${err.localizedMessage}"
                }
            )
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
