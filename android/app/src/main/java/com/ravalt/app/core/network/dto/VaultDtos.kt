package com.ravalt.app.core.network.dto

import com.google.gson.annotations.SerializedName

data class VaultItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("encrypted_data") val encryptedData: String,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("version") val version: Int,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateVaultItemRequest(
    @SerializedName("encrypted_data") val encryptedData: String,
    @SerializedName("nonce") val nonce: String
)

data class UpdateVaultItemRequest(
    @SerializedName("encrypted_data") val encryptedData: String,
    @SerializedName("nonce") val nonce: String
)

data class SyncItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("encrypted_data") val encryptedData: String,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("version") val version: Int = 1
)

data class SyncRequest(
    @SerializedName("since") val since: String? = null,
    @SerializedName("items") val items: List<SyncItemDto> = emptyList(),
    @SerializedName("deleted_ids") val deletedIds: List<String> = emptyList()
)

data class SyncResponse(
    @SerializedName("server_items") val serverItems: List<VaultItemDto>,
    @SerializedName("server_time") val serverTime: String
)
