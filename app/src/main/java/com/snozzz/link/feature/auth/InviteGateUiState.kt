package com.snozzz.link.feature.auth

data class InviteGateUiState(
    val inviteKey: String = "",
    val nickname: String = "",
    val pairCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val pairLabel: String = "",
)
