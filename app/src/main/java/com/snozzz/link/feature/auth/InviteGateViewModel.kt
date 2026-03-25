package com.snozzz.link.feature.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snozzz.link.LinkApplication
import com.snozzz.link.core.model.SessionSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InviteGateViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionStore = (application as LinkApplication).sessionStore

    private val _uiState = MutableStateFlow(InviteGateUiState())
    val uiState: StateFlow<InviteGateUiState> = _uiState.asStateFlow()

    init {
        sessionStore.load()?.let { snapshot ->
            _uiState.value = InviteGateUiState(
                nickname = snapshot.nickname,
                pairCode = snapshot.pairCode,
                isAuthenticated = true,
                pairLabel = buildPairLabel(snapshot.nickname, snapshot.pairCode),
            )
        }
    }

    fun onInviteKeyChange(value: String) {
        _uiState.update { it.copy(inviteKey = value.trim(), errorMessage = null) }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value, errorMessage = null) }
    }

    fun onPairCodeChange(value: String) {
        _uiState.update { it.copy(pairCode = value.trim().uppercase(), errorMessage = null) }
    }

    fun unlockPrototype() {
        val current = _uiState.value
        val nickname = current.nickname.trim()
        val pairCode = current.pairCode.trim().uppercase()
        val inviteKey = current.inviteKey.trim()

        when {
            nickname.length < 2 -> {
                _uiState.update { it.copy(errorMessage = "昵称至少需要 2 个字符") }
            }
            pairCode.length !in 4..12 -> {
                _uiState.update { it.copy(errorMessage = "配对码长度需要在 4 到 12 位之间") }
            }
            inviteKey.length < 8 -> {
                _uiState.update { it.copy(errorMessage = "邀请码至少需要 8 位") }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    delay(450)
                    val snapshot = SessionSnapshot(
                        nickname = nickname,
                        pairCode = pairCode,
                        inviteKeyMasked = inviteKey.take(2) + "****" + inviteKey.takeLast(2),
                    )
                    sessionStore.save(snapshot)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            pairLabel = buildPairLabel(snapshot.nickname, snapshot.pairCode),
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        sessionStore.clear()
        _uiState.value = InviteGateUiState()
    }

    private fun buildPairLabel(nickname: String, pairCode: String): String {
        return "$nickname · Pair $pairCode"
    }
}
