package com.snozzz.link.core.model

data class SessionSnapshot(
    val nickname: String,
    val pairCode: String,
    val sessionToken: String,
    val pairId: String,
)
