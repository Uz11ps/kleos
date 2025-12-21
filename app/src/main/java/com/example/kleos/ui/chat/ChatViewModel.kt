package com.example.kleos.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kleos.data.chat.ChatsRepository
import com.example.kleos.data.model.Message
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatsRepository()

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private var pollingJob: Job? = null

    // Не вызываем refresh() в init, чтобы избежать запросов для неавторизованных пользователей
    // refresh() будет вызван явно в ChatFragment когда пользователь откроет чат

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.loadMessages() }
                .onSuccess { loadedMessages ->
                    // Сохраняем начальное сообщение, если оно есть
                    val currentMessages = _messages.value ?: emptyList()
                    val initialMessage = currentMessages.firstOrNull { it.id == "initial" }
                    if (initialMessage != null && loadedMessages.none { it.id == "initial" }) {
                        _messages.value = listOf(initialMessage) + loadedMessages
                    } else {
                        _messages.value = loadedMessages
                    }
                }
                .onFailure { /* можно добавить телеметрию/тосты */ }
        }
    }

    fun sendUserMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.sendMessage(text.trim()) }
                .onSuccess { _messages.value = it }
                .onFailure { /* обработать ошибку UI-способом */ }
        }
    }

    fun startPolling(intervalMs: Long = 3000L) {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                runCatching { repository.loadMessages() }
                    .onSuccess { _messages.value = it }
                delay(intervalMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
    
    fun addInitialMessage(message: Message) {
        val currentMessages = _messages.value ?: emptyList()
        if (currentMessages.none { it.id == message.id }) {
            _messages.value = listOf(message) + currentMessages
        }
    }
}


