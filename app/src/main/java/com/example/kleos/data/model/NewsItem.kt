package com.example.kleos.data.model

data class NewsItem(
    val id: String,
    val title: String,
    val dateText: String,
    val content: String? = null,
    val imageUrl: String? = null
)

