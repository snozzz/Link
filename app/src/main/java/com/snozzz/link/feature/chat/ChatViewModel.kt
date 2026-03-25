package com.snozzz.link.feature.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snozzz.link.LinkApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatRepository = (application as LinkApplication).chatRepository
    private val draftMessage = MutableStateFlow("")

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.observeMessages(),
        draftMessage,
    ) { messages, draft ->
        ChatUiState(
            messages = messages,
            draftMessage = draft,
            partnerStatus = if (messages.isEmpty()) {
                "等待首次同步"
            } else {
                "本地聊天已可用，下一步接服务器同步"
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(),
    )

    fun onDraftChange(value: String) {
        draftMessage.value = value
    }

    fun sendDraft() {
        val text = draftMessage.value.trim()
        if (text.isEmpty()) return
        chatRepository.sendMessage(text)
        draftMessage.value = ""
    }
}
