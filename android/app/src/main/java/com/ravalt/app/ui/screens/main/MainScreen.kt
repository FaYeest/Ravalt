package com.ravalt.app.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ravalt.app.domain.model.VaultItem
import com.ravalt.app.ui.navigation.MainTab
import com.ravalt.app.ui.screens.breach.BreachScreen
import com.ravalt.app.ui.screens.breach.BreachViewModel
import com.ravalt.app.ui.screens.generator.GeneratorScreen
import com.ravalt.app.ui.screens.generator.GeneratorViewModel
import com.ravalt.app.ui.screens.profile.ProfileScreen
import com.ravalt.app.ui.screens.profile.ProfileViewModel
import com.ravalt.app.ui.screens.ssh.SshScreen
import com.ravalt.app.ui.screens.ssh.SshViewModel
import com.ravalt.app.ui.screens.totp.TotpScreen
import com.ravalt.app.ui.screens.totp.TotpViewModel
import com.ravalt.app.ui.screens.vault.VaultDetailScreen
import com.ravalt.app.ui.screens.vault.VaultScreen
import com.ravalt.app.ui.screens.vault.VaultViewModel
import com.ravalt.app.ui.theme.EmeraldPrimary
import com.ravalt.app.ui.theme.ObsidianBorder
import com.ravalt.app.ui.theme.ObsidianSurface
import com.ravalt.app.ui.theme.TextMuted

@Composable
fun MainScreen(
    vaultViewModel: VaultViewModel,
    totpViewModel: TotpViewModel,
    sshViewModel: SshViewModel,
    generatorViewModel: GeneratorViewModel,
    breachViewModel: BreachViewModel,
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.VAULT) }
    var selectedDetailItem by remember { mutableStateOf<VaultItem?>(null) }

    if (selectedDetailItem != null) {
        VaultDetailScreen(
            item = selectedDetailItem!!,
            onBack = { selectedDetailItem = null },
            onDelete = { id ->
                vaultViewModel.deleteItem(id)
                selectedDetailItem = null
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(
                    containerColor = ObsidianSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(64.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.VAULT,
                        onClick = { selectedTab = MainTab.VAULT },
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Brankas", modifier = Modifier.size(20.dp)) },
                        label = { Text("Brankas", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.TOTP,
                        onClick = { selectedTab = MainTab.TOTP },
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = "2FA", modifier = Modifier.size(20.dp)) },
                        label = { Text("2FA", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.SSH,
                        onClick = { selectedTab = MainTab.SSH },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = "SSH", modifier = Modifier.size(20.dp)) },
                        label = { Text("SSH", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.GENERATOR,
                        onClick = { selectedTab = MainTab.GENERATOR },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generator", modifier = Modifier.size(20.dp)) },
                        label = { Text("Generator", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.BREACH,
                        onClick = { selectedTab = MainTab.BREACH },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Breach", modifier = Modifier.size(20.dp)) },
                        label = { Text("Breach", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == MainTab.PROFILE,
                        onClick = { selectedTab = MainTab.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Akun", modifier = Modifier.size(20.dp)) },
                        label = { Text("Akun", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    MainTab.VAULT -> VaultScreen(
                        viewModel = vaultViewModel,
                        onSelectItem = { selectedDetailItem = it }
                    )
                    MainTab.TOTP -> TotpScreen(viewModel = totpViewModel)
                    MainTab.SSH -> SshScreen(viewModel = sshViewModel)
                    MainTab.GENERATOR -> GeneratorScreen(viewModel = generatorViewModel)
                    MainTab.BREACH -> BreachScreen(viewModel = breachViewModel)
                    MainTab.PROFILE -> ProfileScreen(
                        viewModel = profileViewModel,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
