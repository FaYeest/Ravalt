package com.ravalt.app.ui.screens.totp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ravalt.app.core.totp.TotpGenerator
import com.ravalt.app.ui.theme.EmeraldPrimary
import com.ravalt.app.ui.theme.ObsidianBorder
import com.ravalt.app.ui.theme.ObsidianSurface
import com.ravalt.app.ui.theme.TextMuted

@Composable
fun AddTotpDialog(
    onDismiss: () -> Unit,
    onAdd: (issuer: String, account: String, secret: String) -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var rawUri by remember { mutableStateOf("") }
    var isQrTab by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tambah Akun 2FA Authenticator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Input Manual vs Scan / URI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Button(
                        onClick = { isQrTab = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isQrTab) ObsidianSurface else Color.Transparent,
                            contentColor = if (!isQrTab) Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Kode Manual")
                    }

                    Button(
                        onClick = { isQrTab = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isQrTab) ObsidianSurface else Color.Transparent,
                            contentColor = if (isQrTab) Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Scan / URI")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isQrTab) {
                    OutlinedTextField(
                        value = rawUri,
                        onValueChange = {
                            rawUri = it
                            val parsed = TotpGenerator.parseOtpUri(it)
                            if (parsed != null) {
                                secret = parsed.secret
                                issuer = parsed.issuer ?: ""
                                account = parsed.accountName ?: ""
                            }
                        },
                        label = { Text("Tautan QR Code (otpauth://...)") },
                        placeholder = { Text("otpauth://totp/Google:user@gmail.com?secret=JBSWY3...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = ObsidianBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tip: Tempel teks hasil scan QR code kamera untuk deteksi otomatis parameter kunci.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Layanan / Akun") },
                    placeholder = { Text("cth. GitHub, Google, AWS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("Nama Pengguna (Opsional)") },
                    placeholder = { Text("cth. user@example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text("Kunci Rahasia (Secret Key)") },
                    placeholder = { Text("cth. JBSWY3DPEHPK3PXP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ObsidianBorder
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Batal", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (secret.isNotBlank()) {
                                onAdd(issuer, account, secret)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = secret.isNotBlank()
                    ) {
                        Text(text = "Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
