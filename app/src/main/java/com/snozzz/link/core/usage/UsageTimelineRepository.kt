package com.snozzz.link.core.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem
import com.snozzz.link.core.model.UsageTimelineSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UsageTimelineRepository(
    private val context: Context,
) {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager: PackageManager = context.packageManager
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun loadTodaySnapshot(nowMillis: Long = System.currentTimeMillis()): UsageTimelineSnapshot {
        val startOfDayMillis = LocalDate.now(zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val events = usageStatsManager.queryEvents(startOfDayMillis, nowMillis)
        val activeForegroundStarts = mutableMapOf<String, Long>()
        val totalForegroundDurations = mutableMapOf<String, Long>()
        val timelineItems = mutableListOf<UsageTimelineEventItem>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    activeForegroundStarts[packageName] = event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val foregroundStart = activeForegroundStarts.remove(packageName)
                    val durationMillis = if (foregroundStart != null) {
                        (event.timeStamp - foregroundStart).coerceAtLeast(0L)
                    } else {
                        null
                    }
                    if (durationMillis != null) {
                        totalForegroundDurations[packageName] =
                            totalForegroundDurations.getOrDefault(packageName, 0L) + durationMillis
                    }
                    timelineItems += UsageTimelineEventItem(
                        appName = resolveAppName(packageName),
                        packageName = packageName,
                        timeLabel = formatTime(event.timeStamp),
                        durationLabel = durationMillis?.let(::formatDuration),
                    )
                }
            }
        }

        activeForegroundStarts.forEach { (packageName, foregroundStart) ->
            totalForegroundDurations[packageName] =
                totalForegroundDurations.getOrDefault(packageName, 0L) + (nowMillis - foregroundStart).coerceAtLeast(0L)
        }

        val topApps = totalForegroundDurations
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { (packageName, durationMillis) ->
                AppUsageSummaryItem(
                    appName = resolveAppName(packageName),
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

    private fun resolveAppName(packageName: String): String {
        val packageAlias = knownPackageAliases[packageName]
        val packagePrefixAlias = knownPackagePrefixAliases.entries
            .firstOrNull { (prefix, _) -> packageName.startsWith(prefix) }
            ?.value

        val label = runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrNull()

        return when {
            label.isNullOrBlank() -> packageAlias ?: packagePrefixAlias ?: packageName.substringAfterLast('.')
            label == packageName.substringAfterLast('.') -> packageAlias ?: packagePrefixAlias ?: label
            else -> label
        }
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
        val knownPackageAliases = mapOf(
            "com.ss.android.ugc.aweme" to "抖音",
            "com.tencent.mm" to "微信",
            "com.tencent.mobileqq" to "QQ",
            "com.xingin.xhs" to "小红书",
            "tv.danmaku.bili" to "哔哩哔哩",
        )

        val knownPackagePrefixAliases = mapOf(
            "com.ss.android.ugc.aweme" to "抖音",
            "com.tencent.mm" to "微信",
            "com.tencent.mobileqq" to "QQ",
        )
    }
}
