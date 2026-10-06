package com.ravalt.app.core.network.dto

import com.google.gson.annotations.SerializedName

data class PreloginResponse(
    @SerializedName("salt") val salt: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("salt") val salt: String,
    @SerializedName("auth_hash") val authHash: String
)

data class RegisterResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("auth_hash") val authHash: String
)

data class UserSummary(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String
)

data class LoginResponse(
    @SerializedName("token") val token: String,
    @SerializedName("token_type") val tokenType: String? = null,
    @SerializedName("expires_in") val expiresIn: Long = 0L,
    @SerializedName("user") val user: UserSummary
)

data class UpdatePasswordRequest(
    @SerializedName("new_auth_hash") val newAuthHash: String,
    @SerializedName("new_salt") val newSalt: String
)

data class MessageResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)
