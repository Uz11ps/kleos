package com.example.kleos.data.chat

import com.example.kleos.data.model.Message
import com.example.kleos.data.network.ApiClient
import com.example.kleos.data.network.ChatsApi
import com.example.kleos.data.network.SendMessageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatsRepository {
    private val api = ApiClient.retrofit.create(ChatsApi::class.java)

    private var cachedChatId: String? = null

    suspend fun ensureChatId(): String = withContext(Dispatchers.IO) {
        cachedChatId?.let { return@withContext it }
        val existing = runCatching { api.myChats() }.getOrElse { emptyList() }
        val open = existing.firstOrNull()
        val id = if (open != null) open.id else api.create().id
        cachedChatId = id
        id
    }

    suspend fun loadMessages(): List<Message> = withContext(Dispatchers.IO) {
        val chatId = ensureChatId()
        val dtos = api.messages(chatId)
        dtos.map {
            Message(
                id = it.id,
                sender = if (it.senderRole == "admin") "support" else "user",
                text = it.text,
                timestampMillis = 0L
            )
        }
    }

    suspend fun sendMessage(text: String): List<Message> = withContext(Dispatchers.IO) {
        val chatId = ensureChatId()
        api.send(chatId, SendMessageRequest(text))
        loadMessages()
    }
}



