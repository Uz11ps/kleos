package com.kleos.education.data.model

data class Message(
    val id: String,
    val sender: String, // "user" or "support"
    val text: String,
    val timestampMillis: Long
)


