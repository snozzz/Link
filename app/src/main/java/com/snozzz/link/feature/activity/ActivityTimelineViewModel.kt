package com.snozzz.link.feature.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snozzz.link.LinkApplication
import com.snozzz.link.core.model.UsageTimelineEventItem
import com.snozzz.link.core.model.UsageTimelineSnapshot
import com.snozzz.link.core.network.BackendUsageEvent
import com.snozzz.link.core.usage.UsageAccessController
import com.snozzz.link.core.usage.UsageTimelineRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActivityTimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LinkApplication
    private val usageAccessController = UsageAccessController(application)
    private val usageTimelineRepository = UsageTimelineRepository(application)
    private val sessionStore = app.sessionStore
    private val backendClient = app.backendClient
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private val _uiState = MutableStateFlow(ActivityTimelineUiState())
    val uiState: StateFlow<ActivityTimelineUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val session = sessionStore.load()
            val hasUsageAccess = usageAccessController.hasAccess()
            var snapshot: UsageTimelineSnapshot? = null
            var localSyncMessage = when {
                session == null -> "请先登录后再同步 Moments"
                hasUsageAccess -> "正在上传今天的使用记录…"
                else -> "打开 Usage Access 后才能同步今天的使用记录"
            }

            if (hasUsageAccess) {
                snapshot = usageTimelineRepository.loadTodaySnapshot()
                if (session != null) {
                    localSyncMessage = try {
                        backendClient.uploadUsage(
                            sessionToken = session.sessionToken,
                            pairId = session.pairId,
                            capturedAtEpochMillis = System.currentTimeMillis(),
                            events = snapshot.recentEvents.map { item ->
                                BackendUsageEvent(
                                    appName = item.appName,
                                    packageName = item.packageName,
                                    timeLabel = item.timeLabel,
                                    durationLabel = item.durationLabel,
                                )
                            },
                        )
                        "本地 Moments 已同步到服务器"
                    } catch (exception: Exception) {
                        "本地 Moments 上传失败，稍后会再试"
                    }
                }
            }

            var partnerNickname = "对方"
            var partnerStatus = if (session == null) {
                "请先登录后再查看对方动态"
            } else {
                "正在读取对方最新 Moments…"
            }
            var partnerRefreshedAtLabel = "--:--"
            var partnerEvents: List<UsageTimelineEventItem> = emptyList()

            if (session != null) {
                try {
                    val pairStatus = backendClient.fetchPairStatus(
                        sessionToken = session.sessionToken,
                        pairId = session.pairId,
                    )
                    partnerNickname = pairStatus.partnerNickname
                    val latestUsage = backendClient.fetchLatestUsage(
                        sessionToken = session.sessionToken,
                        pairId = session.pairId,
                    )
                    when {
                        pairStatus.memberCount < 2 -> {
                            partnerStatus = "对方还没加入这组邀请码"
                        }
                        latestUsage == null -> {
                            partnerStatus = "对方今天还没有同步 Moments"
                        }
                        else -> {
                            partnerNickname = latestUsage.ownerNickname
                            partnerRefreshedAtLabel = formatTime(latestUsage.capturedAtEpochMillis)
                            partnerEvents = latestUsage.events.map { event ->
                                UsageTimelineEventItem(
                                    appName = event.appName,
                                    packageName = event.packageName,
                                    timeLabel = event.timeLabel,
                                    durationLabel = event.durationLabel,
                                )
                            }
                            partnerStatus = "${latestUsage.ownerNickname} 最近一次在 $partnerRefreshedAtLabel 同步了动态"
                        }
                    }
                } catch (exception: Exception) {
                    partnerStatus = "读取对方动态失败，稍后重试"
                }
            }

            _uiState.value = ActivityTimelineUiState(
                hasUsageAccess = hasUsageAccess,
                topApps = snapshot?.topApps.orEmpty(),
                recentEvents = snapshot?.recentEvents.orEmpty(),
                refreshedAtLabel = snapshot?.refreshedAtLabel ?: "--:--",
                localSyncMessage = localSyncMessage,
                partnerStatus = partnerStatus,
                partnerNickname = partnerNickname,
                partnerRefreshedAtLabel = partnerRefreshedAtLabel,
                partnerEvents = partnerEvents,
            )
        }
    }

    fun openUsageSettings() {
        usageAccessController.openUsageSettings()
    }

    private fun formatTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(zoneId)
            .format(timeFormatter)
    }
}
