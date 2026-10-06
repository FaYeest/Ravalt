package com.ravalt.app.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravalt.app.ui.components.NotionPropertyRow
import com.ravalt.app.ui.components.StatusSwitchRow
import com.ravalt.app.ui.theme.DangerRed
import com.ravalt.app.ui.theme.EmeraldPrimary
import com.ravalt.app.ui.theme.ObsidianBorder
import com.ravalt.app.ui.theme.ObsidianSurface
import com.ravalt.app.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLock: () -> Unit = {},
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val isLoggedOut by viewModel.isLoggedOut.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedOut) {
        if (isLoggedOut) {
            viewModel.resetLoggedOutState()
            onLogout()
        }
    }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Akun & Pengaturan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.email.firstOrNull()?.uppercase() ?: "F",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = state.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "v1.0.0", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                }
                            }
                            Text(
                                text = "Brankas Mandiri (Zero-Knowledge)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldPrimary))
                                Text(text = "Zero-Knowledge Aktif", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            Text(text = "Kunci Lokal", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = EmeraldPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Cryptographic Specifications
            Text(
                text = "SPESIFIKASI KRIPTOGRAFI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NotionPropertyRow(
                        icon = Icons.Default.Lock,
                        label = "Enkripsi Brankas",
                        value = "AES-256-GCM (96b Nonce)",
                        isMono = true
                    )
                    HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    NotionPropertyRow(
                        icon = Icons.Default.Key,
                        label = "KDF Derivasi Kunci",
                        value = "Argon2id (m=64M, t=3, p=4)",
                        isMono = true
                    )
                    HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    NotionPropertyRow(
                        icon = Icons.Default.Shield,
                        label = "Protokol Audit",
                        value = "k-Anonymity (SHA-1 5-Prefix)",
                        isMono = true
                    )
                    HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    NotionPropertyRow(
                        icon = Icons.Default.Cloud,
                        label = "Host Backend",
                        value = "PostgreSQL 16 (Connected)",
                        isMono = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Security Preferences
            Text(
                text = "PREFERENSI KEAMANAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StatusSwitchRow(
                        title = "Buka Kunci Biometrik",
                        description = "Buka cepat via sidik jari / sensor wajah",
                        checked = state.biometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometric(it) }
                    )
                    HorizontalDivider(color = ObsidianBorder, thickness = 0.5.dp)
                    StatusSwitchRow(
                        title = "Audit Latar Belakang",
                        description = "Audit k-Anonymity otomatis saat membuka brankas",
                        checked = state.backgroundAuditEnabled,
                        onCheckedChange = { viewModel.toggleBackgroundAudit(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Data Management & Sync
            Text(
                text = "MANAJEMEN DATA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        enabled = !state.isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Sinkronkan Sekarang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.lockVault()
                            onLock()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Kunci Brankas Sekarang", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(text = "Keluar dari Akun", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        enabled = !state.isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed.copy(alpha = 0.15f),
                            contentColor = DangerRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DangerRed)
                        } else {
                            Text(text = "Hapus Akun & Seluruh Data Brankas", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Akun Permanen?") },
            text = { Text("Tindakan ini tidak dapat dibatalkan. Seluruh ciphertext di backend server dan database lokal akan dihapus seketika.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White)
                ) {
                    Text("Hapus Permanen")
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
