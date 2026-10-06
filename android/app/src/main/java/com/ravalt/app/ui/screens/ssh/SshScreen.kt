package com.ravalt.app.ui.screens.ssh

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravalt.app.core.ssh.SshKeyType
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.ui.theme.DangerRed
import com.ravalt.app.ui.theme.EmeraldPrimary
import com.ravalt.app.ui.theme.ObsidianBorder
import com.ravalt.app.ui.theme.ObsidianSurface
import com.ravalt.app.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshScreen(
    viewModel: SshViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddDialog() },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah SSH Key")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Cari server host, username...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface
                    )
                )
            }

            if (state.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.searchQuery.isBlank()) "Belum Ada Kunci Server / SSH" else "Kunci tidak ditemukan",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tekan tombol + untuk generate pasangan kunci Ed25519 / RSA atau import server key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        SshKeyCard(
                            item = item,
                            onCopyPublicKey = {
                                clipboardManager.setText(AnnotatedString(item.sshPublicKey))
                                Toast.makeText(context, "Public key disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onCopyCommand = {
                                val portPart = if (item.sshPort != 22) "-p ${item.sshPort} " else ""
                                val userPart = if (item.sshUser.isNotBlank()) "${item.sshUser}@" else ""
                                val cmd = "ssh $portPart$userPart${item.sshHost}"
                                clipboardManager.setText(AnnotatedString(cmd))
                                Toast.makeText(context, "Perintah SSH disalin: $cmd", Toast.LENGTH_SHORT).show()
                            },
                            onCopyPrivateKey = {
                                clipboardManager.setText(AnnotatedString(item.sshPrivateKey))
                                Toast.makeText(context, "Private key rahasia disalin", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = {
                                viewModel.deleteKey(item.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddSshDialog(
            onDismiss = { viewModel.closeAddDialog() },
            onGenerate = { title, host, port, user, type, pass ->
                viewModel.generateAndSaveKey(title, host, port, user, type, pass)
            },
            onImport = { title, host, port, user, pub, priv, pass ->
                viewModel.saveImportedKey(title, host, port, user, pub, priv, pass)
            }
        )
    }
}

@Composable
fun SshKeyCard(
    item: VaultItem,
    onCopyPublicKey: () -> Unit,
    onCopyCommand: () -> Unit,
    onCopyPrivateKey: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val hostDesc = "${item.sshUser.ifBlank { "root" }}@${item.sshHost.ifBlank { "localhost" }}:${item.sshPort}"
                        Text(
                            text = hostDesc,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = ObsidianSurface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Salin Private Key (Rahasia)", color = Color.White) },
                            onClick = {
                                showMenu = false
                                onCopyPrivateKey()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Kunci Server", color = DangerRed) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fingerprint Box
            if (item.sshFingerprint.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.sshFingerprint,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "SHA256",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Quick Actions: Copy Public Key & Copy SSH Command
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyPublicKey,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextMuted)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Public Key", fontSize = 11.sp)
                }

                Button(
                    onClick = onCopyCommand,
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Perintah SSH", fontSize = 11.sp)
                }
            }
        }
    }
}
