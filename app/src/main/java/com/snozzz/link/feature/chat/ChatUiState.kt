package com.snozzz.link.feature.chat

import com.snozzz.link.core.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draftMessage: String = "",
    val partnerStatus: String = "Waiting for first sync",
)
