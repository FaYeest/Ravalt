package com.ravalt.app.domain.model

data class BreachResult(
    val isBreached: Boolean,
    val count: Long,
    val prefix: String,
    val passwordLength: Int
)

data class UserSession(
    val userId: String,
    val email: String,
    val token: String,
    val userSalt: String
)
