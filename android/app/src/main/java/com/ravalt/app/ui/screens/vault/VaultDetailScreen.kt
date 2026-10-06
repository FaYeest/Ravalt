package com.ravalt.app.ui.screens.vault

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.ui.components.NotionPropertyRow
import com.ravalt.app.ui.components.RavaltTopBar
import com.ravalt.app.ui.theme.DangerRed
import com.ravalt.app.ui.theme.EmeraldPrimary
import com.ravalt.app.ui.theme.ObsidianBorder
import com.ravalt.app.ui.theme.ObsidianSurface
import com.ravalt.app.ui.theme.TextMuted

@Composable
fun VaultDetailScreen(
    item: VaultItem,
    onBack: () -> Unit,
    onDelete: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    BackHandler { onBack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RavaltTopBar(
                title = item.title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = DangerRed)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title.firstOrNull()?.uppercase() ?: "K",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Terenkripsi AES-256-GCM (Lokal)",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Properties",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Notion Properties Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Username Row
                    if (item.username.isNotBlank()) {
                        NotionPropertyRow(
                            icon = Icons.Default.Person,
                            label = "Username",
                            value = item.username,
                            trailingAction = {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(item.username))
                                    Toast.makeText(context, "Username disalin", Toast.LENGTH_SHORT).show()
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Salin", tint = TextMuted, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                        HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    }

                    // Password Row
                    if (item.password.isNotBlank()) {
                        NotionPropertyRow(
                            icon = Icons.Default.Key,
                            label = "Password",
                            value = if (passwordVisible) item.password else "••••••••••••",
                            isMono = true,
                            trailingAction = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(24.dp)) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Lihat",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(item.password))
                                        Toast.makeText(context, "Password disalin", Toast.LENGTH_SHORT).show()
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Salin", tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    }

                    // Website URL Row
                    if (item.url.isNotBlank()) {
                        NotionPropertyRow(
                            icon = Icons.Default.Language,
                            label = "Website",
                            value = item.url
                        )
                        HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    }

                    // Notes Row
                    if (item.notes.isNotBlank()) {
                        NotionPropertyRow(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "Catatan",
                            value = item.notes
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Kredensial Ini?") },
            text = { Text("Data kredensial '${item.title}' akan dihapus secara permanen dari brankas lokal dan disinkronkan ke server.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(item.id)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextMuted)
                }
            },
            containerColor = ObsidianSurface
        )
    }
}
