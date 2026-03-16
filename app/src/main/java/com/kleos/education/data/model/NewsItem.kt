package com.kleos.education.data.model

data class NewsItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val dateText: String,
    val content: String? = null,
    val imageUrl: String? = null
)


