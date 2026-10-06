package com.ravalt.app.ui.screens.unlock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravalt.app.ui.theme.EmeraldPrimary
import com.ravalt.app.ui.theme.ObsidianBorder
import com.ravalt.app.ui.theme.ObsidianSurface
import com.ravalt.app.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(
    viewModel: UnlockViewModel,
    onUnlockSuccess: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) {
            viewModel.resetUnlockState()
            onUnlockSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // User Avatar Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.email.firstOrNull()?.uppercase() ?: "R",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Buka Kunci Brankas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = state.email.ifEmpty { "farras@ravalt.id" },
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = state.masterPassword,
                onValueChange = { viewModel.onPasswordChanged(it) },
                label = { Text("Master Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = ObsidianBorder
                )
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { viewModel.unlock() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text(text = "Buka Kunci", fontWeight = FontWeight.Bold)
                }
            }

            if (state.isBiometricAvailable) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { /* Biometric prompt */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Buka dengan Sidik Jari", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSwitchAccount) {
                Text(
                    text = "Ganti Akun atau Keluar",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
