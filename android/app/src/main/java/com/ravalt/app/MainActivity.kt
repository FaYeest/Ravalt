package com.ravalt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                val profileState by authRepo.userProfile.collectAsState(initial = null)

                val startDestination = remember(profileState) {
                    if (profileState != null && profileState!!.authToken.isNotBlank()) {
                        Screen.Unlock.route
                    } else {
                        Screen.Onboarding.route
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination
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
                                navController.navigate(Screen.Unlock.route)
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
                                navController.navigate(Screen.Auth.route)
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
