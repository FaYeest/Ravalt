package com.ravalt.app.core.network

import com.ravalt.app.core.network.dto.CreateVaultItemRequest
import com.ravalt.app.core.network.dto.MessageResponse
import com.ravalt.app.core.network.dto.SyncRequest
import com.ravalt.app.core.network.dto.SyncResponse
import com.ravalt.app.core.network.dto.UpdateVaultItemRequest
import com.ravalt.app.core.network.dto.VaultItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface VaultApiService {

    @GET("vault")
    suspend fun getVaultItems(
        @Query("since") since: String? = null
    ): Response<List<VaultItemDto>>

    @POST("vault")
    suspend fun createVaultItem(
        @Body request: CreateVaultItemRequest
    ): Response<VaultItemDto>

    @GET("vault/{id}")
    suspend fun getVaultItem(
        @Path("id") id: String
    ): Response<VaultItemDto>

    @PUT("vault/{id}")
    suspend fun updateVaultItem(
        @Path("id") id: String,
        @Body request: UpdateVaultItemRequest
    ): Response<VaultItemDto>

    @DELETE("vault/{id}")
    suspend fun deleteVaultItem(
        @Path("id") id: String
    ): Response<MessageResponse>

    @POST("vault/sync")
    suspend fun syncVault(
        @Body request: SyncRequest
    ): Response<SyncResponse>
}
