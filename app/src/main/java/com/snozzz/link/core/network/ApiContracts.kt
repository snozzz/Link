package com.snozzz.link.core.network

data class InviteUnlockRequest(
    val inviteKey: String,
    val nickname: String,
    val pairCode: String,
    val devicePublicKey: String,
)

data class InviteUnlockResponse(
    val sessionToken: String,
    val pairId: String,
    val displayName: String,
)

data class PairStatusResponse(
    val pairId: String,
    val partnerNickname: String,
    val usageSharingEnabled: Boolean,
)

data class OutgoingMessagePayload(
    val localId: String,
    val body: String,
    val sentAtEpochMillis: Long,
)

data class MessageSyncRequest(
    val pairId: String,
    val outgoingMessages: List<OutgoingMessagePayload>,
)

data class MessageSyncResponse(
    val acknowledgedMessageIds: List<String>,
)
