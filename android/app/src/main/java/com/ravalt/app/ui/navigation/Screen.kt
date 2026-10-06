package com.ravalt.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Unlock : Screen("unlock")
    data object Main : Screen("main")
    data object VaultDetail : Screen("vault_detail/{itemId}") {
        fun createRoute(itemId: String) = "vault_detail/$itemId"
    }
}

enum class MainTab(val title: String) {
    VAULT("Brankas"),
    TOTP("2FA"),
    SSH("SSH"),
    GENERATOR("Generator"),
    BREACH("Breach"),
    PROFILE("Akun")
}
