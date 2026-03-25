package com.snozzz.link.core.model

data class AppUsageSummaryItem(
    val appName: String,
    val packageName: String,
    val totalMinutes: Int,
)

data class UsageTimelineEventItem(
    val appName: String,
    val packageName: String,
    val timeLabel: String,
    val durationLabel: String?,
)

data class UsageTimelineSnapshot(
    val topApps: List<AppUsageSummaryItem>,
    val recentEvents: List<UsageTimelineEventItem>,
    val refreshedAtLabel: String,
)
