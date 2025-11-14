package com.example.kleos.data.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class ChatDto(
    val id: String,
    val status: String,
    val lastMessageAt: String?
)

data class ChatCreateResponse(val id: String)

data class ChatMessageDto(
    val id: String,
    val senderRole: String,
    val text: String,
    val createdAt: String
)

data class SendMessageRequest(val text: String)

interface ChatsApi {
    @POST("chats")
    suspend fun create(): ChatCreateResponse

    @GET("chats")
    suspend fun myChats(): List<ChatDto>

    @GET("chats/{id}/messages")
    suspend fun messages(@Path("id") id: String): List<ChatMessageDto>

    @POST("chats/{id}/messages")
    suspend fun send(@Path("id") id: String, @Body body: SendMessageRequest): Map<String, Any?>
}



