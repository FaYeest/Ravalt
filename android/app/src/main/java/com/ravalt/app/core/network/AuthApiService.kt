package com.ravalt.app.core.network

import com.ravalt.app.core.network.dto.AuthResponse
import com.ravalt.app.core.network.dto.LoginRequest
import com.ravalt.app.core.network.dto.MessageResponse
import com.ravalt.app.core.network.dto.PreloginResponse
import com.ravalt.app.core.network.dto.RegisterRequest
import com.ravalt.app.core.network.dto.UpdatePasswordRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface AuthApiService {

    @GET("auth/prelogin")
    suspend fun getPreloginSalt(
        @Query("email") email: String
    ): Response<PreloginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @DELETE("auth/me")
    suspend fun deleteAccount(): Response<MessageResponse>

    @PUT("auth/password")
    suspend fun updatePassword(
        @Body request: UpdatePasswordRequest
    ): Response<MessageResponse>
}
