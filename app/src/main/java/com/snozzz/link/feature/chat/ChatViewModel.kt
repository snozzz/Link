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
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val chatRepository = (application as LinkApplication).chatRepository
    private val draftMessage = MutableStateFlow("")

    val uiState: StateFlow<ChatUiState> = combine(
        chatRepository.observeMessages(),
        chatRepository.observePartnerStatus(),
        draftMessage,
    ) { messages, partnerStatus, draft ->
        ChatUiState(
            messages = messages,
            draftMessage = draft,
            partnerStatus = partnerStatus,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChatUiState(),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { chatRepository.refreshMessages() }
        }
    }

    fun onDraftChange(value: String) {
        draftMessage.value = value
    }

    fun sendDraft() {
        val text = draftMessage.value.trim()
        if (text.isEmpty()) return
        draftMessage.value = ""
        viewModelScope.launch {
            chatRepository.sendMessage(text)
        }
    }
}
