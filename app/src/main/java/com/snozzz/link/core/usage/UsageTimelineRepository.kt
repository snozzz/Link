package com.snozzz.link.core.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem
import com.snozzz.link.core.model.UsageTimelineSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UsageTimelineRepository(
    context: Context,
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val labelResolver = AppLabelResolver(context)
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun loadTodaySnapshot(nowMillis: Long = System.currentTimeMillis()): UsageTimelineSnapshot {
        val startOfDayMillis = LocalDate.now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val events = usageStatsManager.queryEvents(startOfDayMillis, nowMillis)
        val totalForegroundDurations = mutableMapOf<String, Long>()
        val timelineItems = mutableListOf<UsageTimelineEventItem>()
        val lastTimelineIndexByPackage = mutableMapOf<String, Int>()
        var currentForegroundPackage: String? = null
        var currentForegroundStart: Long? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (labelResolver.shouldIgnoreTimelinePackage(packageName) || !isForegroundEntryEvent(event.eventType)) {
                continue
            }
            if (packageName == currentForegroundPackage) {
                continue
            }

            val previousPackage = currentForegroundPackage
            val previousStart = currentForegroundStart
            if (previousPackage != null && previousStart != null) {
                val durationMillis = (event.timeStamp - previousStart).coerceAtLeast(0L)
                if (durationMillis > 0L) {
                    totalForegroundDurations[previousPackage] =
                        totalForegroundDurations.getOrDefault(previousPackage, 0L) + durationMillis
                    val previousIndex = lastTimelineIndexByPackage[previousPackage]
                    if (previousIndex != null) {
                        val previousItem = timelineItems[previousIndex]
                        if (previousItem.durationLabel == null) {
                            timelineItems[previousIndex] = previousItem.copy(
                                durationLabel = formatDuration(durationMillis),
                            )
                        }
                    }
                }
            }

            currentForegroundPackage = packageName
            currentForegroundStart = event.timeStamp
            timelineItems += UsageTimelineEventItem(
                appName = labelResolver.resolveAppName(packageName),
                packageName = packageName,
                timeLabel = formatTime(event.timeStamp),
                durationLabel = null,
            )
            lastTimelineIndexByPackage[packageName] = timelineItems.lastIndex
        }

        if (currentForegroundPackage != null && currentForegroundStart != null) {
            val durationMillis = (nowMillis - currentForegroundStart).coerceAtLeast(0L)
            if (durationMillis > 0L) {
                totalForegroundDurations[currentForegroundPackage] =
                    totalForegroundDurations.getOrDefault(currentForegroundPackage, 0L) + durationMillis
                val lastIndex = lastTimelineIndexByPackage[currentForegroundPackage]
                if (lastIndex != null) {
                    val lastItem = timelineItems[lastIndex]
                    if (lastItem.durationLabel == null) {
                        timelineItems[lastIndex] = lastItem.copy(
                            durationLabel = formatDuration(durationMillis),
                        )
                    }
                }
            }
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
            recentEvents = timelineItems.takeLast(12).asReversed(),
            refreshedAtLabel = formatTime(nowMillis),
        )
    }

    private fun isForegroundEntryEvent(eventType: Int): Boolean {
        return eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
            eventType == UsageEvents.Event.ACTIVITY_RESUMED
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
}
