package com.snozzz.link.feature.activity

import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem

data class ActivityTimelineUiState(
    val hasUsageAccess: Boolean = false,
    val topApps: List<AppUsageSummaryItem> = emptyList(),
    val recentEvents: List<UsageTimelineEventItem> = emptyList(),
    val refreshedAtLabel: String = "--:--",
    val localSyncMessage: String = "尚未同步本地 Moments",
    val partnerStatus: String = "正在读取对方动态…",
    val partnerNickname: String = "对方",
    val partnerRefreshedAtLabel: String = "--:--",
    val partnerEvents: List<UsageTimelineEventItem> = emptyList(),
)
