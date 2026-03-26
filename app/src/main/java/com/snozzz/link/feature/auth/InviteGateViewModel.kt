package com.snozzz.link.feature.auth

import android.app.Application
import android.os.Build
import android.provider.Settings
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

        when {
            nickname.length < 2 -> {
                _uiState.update { it.copy(errorMessage = "昵称至少需要 2 个字符") }
            }
            pairCode.length != 4 -> {
                _uiState.update { it.copy(errorMessage = "配对码需要是 4 位，由服务器生成") }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    try {
                        val result = backendClient.unlockPairCode(
                            pairCode = pairCode,
                            nickname = nickname,
                            devicePublicKey = buildDevicePublicKey(),
                        )
                        val snapshot = SessionSnapshot(
                            nickname = result.displayName,
                            pairCode = result.pairCode,
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

    private fun buildDevicePublicKey(): String {
        val context = getApplication<Application>()
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeIf { it.isNotBlank() }
            ?: "unknown-device"
        val model = Build.MODEL.orEmpty().replace(" ", "_")
        return "android-$androidId-$model"
    }

    private fun mapBackendError(exception: LinkBackendException): String {
        return when (exception.detail) {
            "pair_code_not_found" -> "这个配对码不存在，只有服务器生成的配对码才能进入"
            "pair_code_expired" -> "这个配对码已经过期了，请重新生成"
            "pair_code_exhausted", "pair_full" -> "这个配对码已经被两台设备用过，不能再加入"
            else -> "登录失败：${exception.detail ?: exception.message.orEmpty()}"
        }
    }
}
