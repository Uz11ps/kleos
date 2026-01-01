package com.kleos.education.data.model

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String? = null
)


