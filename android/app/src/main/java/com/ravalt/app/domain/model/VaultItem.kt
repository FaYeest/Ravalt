package com.ravalt.app.domain.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID

enum class VaultItemType {
    LOGIN,
    SSH,
    TOTP,
    NOTE,
    CARD
}

data class VaultPayload(
    @SerializedName("type") val type: String = "LOGIN",
    @SerializedName("title") val title: String = "",
    @SerializedName("username") val username: String? = null,
    @SerializedName("password") val password: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("notes") val notes: String? = null,

    // SSH fields
    @SerializedName("ssh_host") val sshHost: String? = null,
    @SerializedName("ssh_port") val sshPort: Int? = 22,
    @SerializedName("ssh_user") val sshUser: String? = null,
    @SerializedName("ssh_public_key") val sshPublicKey: String? = null,
    @SerializedName("ssh_private_key") val sshPrivateKey: String? = null,
    @SerializedName("ssh_passphrase") val sshPassphrase: String? = null,
    @SerializedName("ssh_fingerprint") val sshFingerprint: String? = null,

    // TOTP fields
    @SerializedName("totp_secret") val totpSecret: String? = null,
    @SerializedName("totp_issuer") val totpIssuer: String? = null,
    @SerializedName("totp_account") val totpAccount: String? = null,
    @SerializedName("totp_digits") val totpDigits: Int? = 6,
    @SerializedName("totp_period") val totpPeriod: Int? = 30
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): VaultPayload {
            return try {
                Gson().fromJson(json, VaultPayload::class.java) ?: VaultPayload()
            } catch (_: Exception) {
                VaultPayload()
            }
        }
    }
}

data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val type: VaultItemType = VaultItemType.LOGIN,
    val title: String,
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val notes: String = "",

    // SSH specific
    val sshHost: String = "",
    val sshPort: Int = 22,
    val sshUser: String = "",
    val sshPublicKey: String = "",
    val sshPrivateKey: String = "",
    val sshPassphrase: String = "",
    val sshFingerprint: String = "",

    // TOTP specific
    val totpSecret: String = "",
    val totpIssuer: String = "",
    val totpAccount: String = "",
    val totpDigits: Int = 6,
    val totpPeriod: Int = 30,

    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
) {
    fun toPayload(): VaultPayload {
        return VaultPayload(
            type = type.name,
            title = title,
            username = username.ifBlank { null },
            password = password.ifBlank { null },
            url = url.ifBlank { null },
            notes = notes.ifBlank { null },
            sshHost = sshHost.ifBlank { null },
            sshPort = if (sshPort != 22) sshPort else null,
            sshUser = sshUser.ifBlank { null },
            sshPublicKey = sshPublicKey.ifBlank { null },
            sshPrivateKey = sshPrivateKey.ifBlank { null },
            sshPassphrase = sshPassphrase.ifBlank { null },
            sshFingerprint = sshFingerprint.ifBlank { null },
            totpSecret = totpSecret.ifBlank { null },
            totpIssuer = totpIssuer.ifBlank { null },
            totpAccount = totpAccount.ifBlank { null },
            totpDigits = totpDigits,
            totpPeriod = totpPeriod
        )
    }

    companion object {
        fun fromPayload(id: String, payload: VaultPayload, version: Int = 1, updatedAt: Long = System.currentTimeMillis()): VaultItem {
            val itemType = try {
                VaultItemType.valueOf(payload.type.uppercase())
            } catch (_: Exception) {
                VaultItemType.LOGIN
            }

            return VaultItem(
                id = id,
                type = itemType,
                title = payload.title,
                username = payload.username ?: "",
                password = payload.password ?: "",
                url = payload.url ?: "",
                notes = payload.notes ?: "",
                sshHost = payload.sshHost ?: "",
                sshPort = payload.sshPort ?: 22,
                sshUser = payload.sshUser ?: "",
                sshPublicKey = payload.sshPublicKey ?: "",
                sshPrivateKey = payload.sshPrivateKey ?: "",
                sshPassphrase = payload.sshPassphrase ?: "",
                sshFingerprint = payload.sshFingerprint ?: "",
                totpSecret = payload.totpSecret ?: "",
                totpIssuer = payload.totpIssuer ?: "",
                totpAccount = payload.totpAccount ?: "",
                totpDigits = payload.totpDigits ?: 6,
                totpPeriod = payload.totpPeriod ?: 30,
                version = version,
                updatedAt = updatedAt
            )
        }
    }
}
