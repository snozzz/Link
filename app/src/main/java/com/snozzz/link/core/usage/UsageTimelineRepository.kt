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
        val totalForegroundDurations = mutableMapOf<String, Long>()
        val timelineItems = mutableListOf<UsageTimelineEventItem>()
        val lastTimelineIndexByPackage = mutableMapOf<String, Int>()
        var currentForegroundPackage: String? = null
        var currentForegroundStart: Long? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName ?: continue
            if (!isForegroundEntryEvent(event.eventType)) {
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
                appName = resolveAppName(packageName),
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

    private fun isForegroundEntryEvent(eventType: Int): Boolean {
        return eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
            eventType == UsageEvents.Event.ACTIVITY_RESUMED
    }

    private fun resolveAppName(packageName: String): String {
        val label = runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrNull()

        val exactAlias = knownPackageAliases[packageName]
        val prefixAlias = knownPackagePrefixAliases.entries
            .firstOrNull { (prefix, _) -> packageName.startsWith(prefix) }
            ?.value

        val normalizedAlias = sequenceOf(
            label,
            packageName,
            packageName.substringAfterLast('.'),
        ).filterNotNull()
            .map(::normalizeToAlias)
            .firstOrNull()

        return when {
            !normalizedAlias.isNullOrBlank() -> normalizedAlias
            !exactAlias.isNullOrBlank() -> exactAlias
            !prefixAlias.isNullOrBlank() -> prefixAlias
            !label.isNullOrBlank() && label != packageName.substringAfterLast('.') -> label
            else -> packageName.substringAfterLast('.')
        }
    }

    private fun normalizeToAlias(value: String): String? {
        val trimmed = value.trim()
        val lower = trimmed.lowercase()
        return when {
            trimmed.contains("抖音") ||
                lower == "aweme" ||
                lower.contains("ugc.aweme") ||
                lower.contains("douyin") ||
                lower.contains("amemv") -> "抖音"

            trimmed.contains("微信") ||
                lower == "mm" ||
                lower.contains("tencent.mm") ||
                lower.contains("wechat") ||
                lower.contains("weixin") -> "微信"

            trimmed.contains("QQ") ||
                lower == "qq" ||
                lower.contains("mobileqq") ||
                lower.contains("tencent.mobileqq") -> "QQ"

            trimmed.contains("小红书") ||
                lower.contains("xingin") ||
                lower.contains("xiaohongshu") -> "小红书"

            trimmed.contains("哔哩") ||
                lower.contains("bili") -> "哔哩哔哩"

            else -> null
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
