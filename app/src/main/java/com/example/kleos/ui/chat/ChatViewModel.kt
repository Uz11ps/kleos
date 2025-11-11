package com.example.kleos.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kleos.data.model.Message
import java.util.UUID

class ChatViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        val newMessage = Message(
            id = UUID.randomUUID().toString(),
            sender = "user",
            text = text,
            timestampMillis = System.currentTimeMillis()
        )
        _messages.value = (_messages.value ?: emptyList()) + newMessage
        // Simulate support auto-reply
        val reply = Message(
            id = UUID.randomUUID().toString(),
            sender = "support",
            text = "Спасибо! Мы свяжемся с вами.",
            timestampMillis = System.currentTimeMillis()
        )
        _messages.value = (_messages.value ?: emptyList()) + reply
    }
}


