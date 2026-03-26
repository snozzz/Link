package com.snozzz.link.feature.auth

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snozzz.link.LinkApplication
import com.snozzz.link.core.model.SessionSnapshot
import com.snozzz.link.core.network.LinkBackendException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InviteGateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LinkApplication
    private val sessionStore = app.sessionStore
    private val backendClient = app.backendClient

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

    fun unlockInvite() {
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
                    try {
                        val result = backendClient.unlockInvite(
                            inviteKey = inviteKey,
                            pairCode = pairCode,
                            nickname = nickname,
                            devicePublicKey = buildDevicePublicKey(nickname, pairCode),
                        )
                        val snapshot = SessionSnapshot(
                            nickname = result.displayName,
                            pairCode = result.pairCode,
                            inviteKeyMasked = inviteKey.take(2) + "****" + inviteKey.takeLast(2),
                            sessionToken = result.sessionToken,
                            pairId = result.pairId,
                        )
                        sessionStore.save(snapshot)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                pairLabel = buildPairLabel(snapshot.nickname, snapshot.pairCode),
                            )
                        }
                    } catch (exception: LinkBackendException) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = mapBackendError(exception),
                            )
                        }
                    } catch (exception: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "连接服务器失败，请检查网络或稍后重试",
                            )
                        }
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

    private fun buildDevicePublicKey(nickname: String, pairCode: String): String {
        val model = Build.MODEL.orEmpty().replace(" ", "_")
        return "android-$model-$nickname-$pairCode-${System.currentTimeMillis()}"
    }

    private fun mapBackendError(exception: LinkBackendException): String {
        return when (exception.detail) {
            "invite_not_found" -> "邀请码不存在，请确认输入无误"
            "pair_code_mismatch" -> "邀请码和配对码不匹配"
            "invite_expired" -> "邀请码已过期，请重新生成"
            "invite_exhausted" -> "这组邀请码已经用满了"
            "pair_full" -> "这组配对已经有两个人了"
            else -> "登录失败：${exception.detail ?: exception.message.orEmpty()}"
        }
    }
}
