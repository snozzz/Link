package com.snozzz.link.core.chat

import com.snozzz.link.core.model.ChatMessage
import com.snozzz.link.core.model.MessageAuthor
import com.snozzz.link.core.model.MessageDeliveryState
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryChatRepository {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val messages = MutableStateFlow(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "今天的 usage timeline 已经可以读本地数据了，晚点接服务端。",
                sentAtLabel = "20:14",
                author = MessageAuthor.PARTNER,
                deliveryState = MessageDeliveryState.SYNCED,
            ),
            ChatMessage(
                id = UUID.randomUUID().toString(),
                text = "好，我先把邀请码和聊天都串起来。",
                sentAtLabel = "20:16",
                author = MessageAuthor.ME,
                deliveryState = MessageDeliveryState.LOCAL_ONLY,
            ),
        ),
    )

    fun observeMessages(): StateFlow<List<ChatMessage>> = messages.asStateFlow()

    fun sendMessage(text: String) {
        val sanitized = text.trim()
        if (sanitized.isEmpty()) return
        val nowLabel = LocalTime.now().format(timeFormatter)
        val pendingId = UUID.randomUUID().toString()
        messages.update { current ->
            current + ChatMessage(
                id = pendingId,
                text = sanitized,
                sentAtLabel = nowLabel,
                author = MessageAuthor.ME,
                deliveryState = MessageDeliveryState.SYNCING,
            )
        }
        messages.update { current ->
            current.map { message ->
                if (message.id == pendingId) {
                    message.copy(deliveryState = MessageDeliveryState.LOCAL_ONLY)
                } else {
                    message
                }
            }
        }
    }
}
