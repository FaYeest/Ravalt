package com.ravalt.app.core.network.dto

import com.google.gson.annotations.SerializedName

data class PreloginResponse(
    @SerializedName("salt") val salt: String
)

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("auth_hash") val authHash: String,
    @SerializedName("user_salt") val userSalt: String
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("auth_hash") val authHash: String
)

data class AuthResponse(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("token") val token: String
)

data class UpdatePasswordRequest(
    @SerializedName("new_auth_hash") val newAuthHash: String,
    @SerializedName("new_user_salt") val newUserSalt: String
)

data class MessageResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null
)
