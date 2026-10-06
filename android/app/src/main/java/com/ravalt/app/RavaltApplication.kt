package com.ravalt.app

import android.app.Application
import com.ravalt.app.core.network.ApiClient
import com.ravalt.app.core.session.SessionManager
import com.ravalt.app.data.local.AppDatabase
import com.ravalt.app.data.repository.AuthRepository
import com.ravalt.app.data.repository.BreachRepository
import com.ravalt.app.data.repository.VaultRepository

class RavaltApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var vaultRepository: VaultRepository
        private set

    lateinit var breachRepository: BreachRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        apiClient = ApiClient { SessionManager.activeToken }

        authRepository = AuthRepository(
            apiClient = apiClient,
            userProfileDao = database.userProfileDao(),
            vaultDao = database.vaultDao()
        )

        vaultRepository = VaultRepository(
            apiClient = apiClient,
            vaultDao = database.vaultDao(),
            userProfileDao = database.userProfileDao()
        )

        breachRepository = BreachRepository(
            apiClient = apiClient
        )
    }

    companion object {
        lateinit var instance: RavaltApplication
            private set
    }
}
