package com.snozzz.link.core.chat

import com.snozzz.link.core.model.ChatMessage
import com.snozzz.link.core.model.MessageAuthor
import com.snozzz.link.core.model.MessageDeliveryState
import com.snozzz.link.core.network.BackendMessage
import com.snozzz.link.core.network.LinkBackendClient
import com.snozzz.link.core.network.OutgoingMessagePayload
import com.snozzz.link.core.security.SecureSessionStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackendChatRepository(
    private val backendClient: LinkBackendClient,
    private val sessionStore: SecureSessionStore,
) {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val partnerStatus = MutableStateFlow("等待消息同步")

    fun observeMessages(): StateFlow<List<ChatMessage>> = messages.asStateFlow()

    fun observePartnerStatus(): StateFlow<String> = partnerStatus.asStateFlow()

    suspend fun refreshMessages() {
        val session = sessionStore.load() ?: return
        val pairStatus = backendClient.fetchPairStatus(
            sessionToken = session.sessionToken,
            pairId = session.pairId,
        )
        val sync = backendClient.syncMessages(
            sessionToken = session.sessionToken,
            pairId = session.pairId,
            outgoingMessages = emptyList(),
        )
        messages.value = sync.messages.map(::toChatMessage)
        partnerStatus.value = when {
            pairStatus.memberCount < 2 -> "对方还没加入这组邀请码"
            else -> "已连接 ${pairStatus.partnerNickname}，消息会同步到服务器"
        }
    }

    suspend fun sendMessage(text: String) {
        val session = sessionStore.load() ?: return
        val sanitized = text.trim()
        if (sanitized.isEmpty()) return

        val localId = UUID.randomUUID().toString()
        val nowMillis = System.currentTimeMillis()
        val pendingMessage = ChatMessage(
            id = localId,
            text = sanitized,
            sentAtLabel = formatTime(nowMillis),
            author = MessageAuthor.ME,
            deliveryState = MessageDeliveryState.SYNCING,
        )
        messages.value = messages.value + pendingMessage

        try {
            val sync = backendClient.syncMessages(
                sessionToken = session.sessionToken,
                pairId = session.pairId,
                outgoingMessages = listOf(
                    OutgoingMessagePayload(
                        localId = localId,
                        body = sanitized,
                        sentAtEpochMillis = nowMillis,
                    ),
                ),
            )
            messages.value = sync.messages.map(::toChatMessage)
            val pairStatus = backendClient.fetchPairStatus(
                sessionToken = session.sessionToken,
                pairId = session.pairId,
            )
            partnerStatus.value = when {
                pairStatus.memberCount < 2 -> "消息已上传，等待对方加入"
                else -> "消息已同步给 ${pairStatus.partnerNickname}"
            }
        } catch (exception: Exception) {
            messages.value = messages.value.map { message ->
                if (message.id == localId) {
                    message.copy(deliveryState = MessageDeliveryState.LOCAL_ONLY)
                } else {
                    message
                }
            }
            partnerStatus.value = "消息同步失败，稍后重试"
        }
    }

    private fun toChatMessage(message: BackendMessage): ChatMessage {
        return ChatMessage(
            id = message.id,
            text = message.body,
            sentAtLabel = formatTime(message.sentAtEpochMillis),
            author = if (message.fromMe) MessageAuthor.ME else MessageAuthor.PARTNER,
            deliveryState = MessageDeliveryState.SYNCED,
        )
    }

    private fun formatTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(zoneId)
            .format(timeFormatter)
    }
}
