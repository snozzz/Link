package com.snozzz.link.core.model

enum class MessageAuthor {
    ME,
    PARTNER,
}

enum class MessageDeliveryState {
    LOCAL_ONLY,
    SYNCING,
    SYNCED,
}

data class ChatMessage(
    val id: String,
    val text: String,
    val sentAtLabel: String,
    val author: MessageAuthor,
    val deliveryState: MessageDeliveryState,
)
