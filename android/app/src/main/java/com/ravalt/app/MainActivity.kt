package com.ravalt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ravalt.app.ui.theme.EmeraldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ravalt.app.ui.navigation.Screen
import com.ravalt.app.ui.screens.auth.AuthScreen
import com.ravalt.app.ui.screens.auth.AuthViewModel
import com.ravalt.app.ui.screens.breach.BreachViewModel
import com.ravalt.app.ui.screens.generator.GeneratorViewModel
import com.ravalt.app.ui.screens.main.MainScreen
import com.ravalt.app.ui.screens.onboarding.OnboardingScreen
import com.ravalt.app.ui.screens.profile.ProfileViewModel
import com.ravalt.app.ui.screens.ssh.SshViewModel
import com.ravalt.app.ui.screens.totp.TotpViewModel
import com.ravalt.app.ui.screens.unlock.UnlockScreen
import com.ravalt.app.ui.screens.unlock.UnlockViewModel
import com.ravalt.app.ui.screens.vault.VaultViewModel
import com.ravalt.app.ui.theme.RavaltTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = RavaltApplication.instance
        val authRepo = app.authRepository
        val vaultRepo = app.vaultRepository
        val breachRepo = app.breachRepository
        val userProfileDao = app.database.userProfileDao()

        val authViewModel = AuthViewModel(authRepo)
        val unlockViewModel = UnlockViewModel(authRepo)
        val vaultViewModel = VaultViewModel(vaultRepo)
        val totpViewModel = TotpViewModel(vaultRepo)
        val sshViewModel = SshViewModel(vaultRepo)
        val generatorViewModel = GeneratorViewModel()
        val breachViewModel = BreachViewModel(breachRepo)
        val profileViewModel = ProfileViewModel(authRepo, vaultRepo, userProfileDao)

        setContent {
            RavaltTheme {
                val navController = rememberNavController()
                var startupDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val profile = withContext(Dispatchers.IO) { userProfileDao.getProfileSync() }
                    startupDestination = if (profile != null) {
                        Screen.Unlock.route
                    } else {
                        Screen.Onboarding.route
                    }
                }

                if (startupDestination == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = startupDestination!!
                    ) {
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(
                                onGetStarted = {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Auth.route) {
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                },
                                onNavigateToUnlock = {
                                    navController.navigate(Screen.Unlock.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Unlock.route) {
                            UnlockScreen(
                                viewModel = unlockViewModel,
                                onUnlockSuccess = {
                                    navController.navigate(Screen.Main.route) {
                                        popUpTo(Screen.Unlock.route) { inclusive = true }
                                    }
                                },
                                onSwitchAccount = {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Unlock.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Main.route) {
                            MainScreen(
                                vaultViewModel = vaultViewModel,
                                totpViewModel = totpViewModel,
                                sshViewModel = sshViewModel,
                                generatorViewModel = generatorViewModel,
                                breachViewModel = breachViewModel,
                                profileViewModel = profileViewModel,
                                onLock = {
                                    navController.navigate(Screen.Unlock.route) {
                                        popUpTo(Screen.Main.route) { inclusive = true }
                                    }
                                },
                                onLogout = {
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(Screen.Main.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
