package com.snozzz.link.feature.activity

import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem

data class ActivityTimelineUiState(
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    val timelineSourceLabel: String = "未开启前台记录",
    val topApps: List<AppUsageSummaryItem> = emptyList(),
    val recentEvents: List<UsageTimelineEventItem> = emptyList(),
    val refreshedAtLabel: String = "--:--",
) {
    val canReadMoments: Boolean
        get() = hasUsageAccess || hasAccessibilityAccess
}
