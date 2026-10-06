package com.ravalt.app.ui.screens.ssh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ravalt.app.core.ssh.SshKeyGenerator
import com.ravalt.app.core.ssh.SshKeyType
import com.ravalt.app.data.repository.VaultRepository
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.domain.model.VaultItemType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SshUiState(
    val items: List<VaultItem> = emptyList(),
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val toastMessage: String? = null
)

class SshViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showAddDialog = MutableStateFlow(false)
    private val _toastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SshUiState> = combine(
        vaultRepository.vaultItems,
        _searchQuery,
        _showAddDialog,
        _toastMessage
    ) { items, query, showAdd, toast ->
        val sshItems = items.filter { it.type == VaultItemType.SSH }
        val filtered = sshItems.filter { item ->
            query.isBlank() ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.sshHost.contains(query, ignoreCase = true) ||
                    item.sshUser.contains(query, ignoreCase = true)
        }

        SshUiState(
            items = filtered,
            searchQuery = query,
            showAddDialog = showAdd,
            toastMessage = toast
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SshUiState())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun openAddDialog() {
        _showAddDialog.value = true
    }

    fun closeAddDialog() {
        _showAddDialog.value = false
    }

    fun generateAndSaveKey(
        title: String,
        host: String,
        port: Int,
        user: String,
        type: SshKeyType,
        passphrase: String
    ) {
        viewModelScope.launch {
            val keyPair = SshKeyGenerator.generateKeyPair(type, comment = "$user@$host")
            val item = VaultItem(
                title = title.ifBlank { "SSH Key ($host)" },
                type = VaultItemType.SSH,
                sshHost = host.trim(),
                sshPort = port,
                sshUser = user.trim(),
                sshPublicKey = keyPair.publicKeyOpenSsh,
                sshPrivateKey = keyPair.privateKeyPem,
                sshFingerprint = keyPair.fingerprintSha256,
                sshPassphrase = passphrase.trim()
            )
            vaultRepository.saveItem(item)
            _showAddDialog.value = false
            _toastMessage.value = "Kunci SSH (${type.name}) berhasil digenerate dan disimpan aman"
        }
    }

    fun saveImportedKey(
        title: String,
        host: String,
        port: Int,
        user: String,
        publicKey: String,
        privateKey: String,
        passphrase: String
    ) {
        viewModelScope.launch {
            val item = VaultItem(
                title = title.ifBlank { "SSH Key ($host)" },
                type = VaultItemType.SSH,
                sshHost = host.trim(),
                sshPort = port,
                sshUser = user.trim(),
                sshPublicKey = publicKey.trim(),
                sshPrivateKey = privateKey.trim(),
                sshFingerprint = "SHA256:imported-key",
                sshPassphrase = passphrase.trim()
            )
            vaultRepository.saveItem(item)
            _showAddDialog.value = false
            _toastMessage.value = "Kunci SSH berhasil disimpan ke brankas"
        }
    }

    fun deleteKey(id: String) {
        viewModelScope.launch {
            vaultRepository.deleteItem(id)
            _toastMessage.value = "Kunci SSH dihapus"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
