package com.snozzz.link.feature.activity

import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem

data class ActivityTimelineUiState(
    val hasUsageAccess: Boolean = false,
    val topApps: List<AppUsageSummaryItem> = emptyList(),
    val recentEvents: List<UsageTimelineEventItem> = emptyList(),
    val refreshedAtLabel: String = "--:--",
)
