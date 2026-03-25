package com.snozzz.link.core.accessibility

import android.content.Context
import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem
import com.snozzz.link.core.model.UsageTimelineSnapshot
import com.snozzz.link.core.usage.AppLabelResolver
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AccessibilityTimelineRepository(
    context: Context,
) {
    private val store = AccessibilityTimelineStore(context)
    private val labelResolver = AppLabelResolver(context)
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun loadTodaySnapshot(nowMillis: Long = System.currentTimeMillis()): UsageTimelineSnapshot {
        store.clearBefore(TimelineClock.startOfTodayMillis())
        val rawEvents = store.loadTodayEvents()
            .filterNot { labelResolver.shouldIgnoreTimelinePackage(it.packageName) }
        if (rawEvents.isEmpty()) {
            return UsageTimelineSnapshot(
                topApps = emptyList(),
                recentEvents = emptyList(),
                refreshedAtLabel = formatTime(nowMillis),
            )
        }

        val totalForegroundDurations = mutableMapOf<String, Long>()
        val timelineItems = rawEvents.mapIndexed { index, event ->
            val nextEvent = rawEvents.getOrNull(index + 1)
            val durationMillis = nextEvent?.let { candidate ->
                (candidate.timestampMillis - event.timestampMillis)
                    .coerceAtLeast(0L)
                    .coerceAtMost(MAX_SESSION_MILLIS)
            }
            if (durationMillis != null && durationMillis > 0L) {
                totalForegroundDurations[event.packageName] =
                    totalForegroundDurations.getOrDefault(event.packageName, 0L) + durationMillis
            }
            UsageTimelineEventItem(
                appName = labelResolver.resolveAppName(event.packageName),
                packageName = event.packageName,
                timeLabel = formatTime(event.timestampMillis),
                durationLabel = durationMillis?.takeIf { it > 0L }?.let(::formatDuration),
            )
        }

        val topApps = totalForegroundDurations
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (packageName, durationMillis) ->
                AppUsageSummaryItem(
                    appName = labelResolver.resolveAppName(packageName),
                    packageName = packageName,
                    totalMinutes = (durationMillis / 60000L).toInt().coerceAtLeast(1),
                )
            }

        return UsageTimelineSnapshot(
            topApps = topApps,
            recentEvents = timelineItems.takeLast(16).asReversed(),
            refreshedAtLabel = formatTime(nowMillis),
        )
    }

    private fun formatTime(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(zoneId)
            .format(timeFormatter)
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = (durationMillis / 60000L).toInt()
        return when {
            totalMinutes >= 60 -> {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                if (minutes == 0) "${hours}h" else "${hours}h ${minutes}m"
            }
            totalMinutes > 0 -> "${totalMinutes}m"
            else -> "<1m"
        }
    }

    private companion object {
        const val MAX_SESSION_MILLIS = 2 * 60 * 60 * 1000L
    }
}
