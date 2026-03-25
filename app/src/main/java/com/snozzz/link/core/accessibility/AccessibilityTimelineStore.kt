package com.snozzz.link.core.accessibility

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AccessibilityTimelineStore(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun recordForegroundPackage(packageName: String, timestampMillis: Long): Boolean {
        val events = loadEventsLocked().toMutableList()
        val lastEvent = events.lastOrNull()
        if (lastEvent != null && lastEvent.packageName == packageName &&
            timestampMillis - lastEvent.timestampMillis < DUPLICATE_WINDOW_MILLIS
        ) {
            return false
        }
        events += AccessibilityTimelineEntry(
            packageName = packageName,
            timestampMillis = timestampMillis,
        )
        val trimmedEvents = events.takeLast(MAX_EVENTS)
        saveEventsLocked(trimmedEvents)
        return true
    }

    @Synchronized
    fun loadTodayEvents(): List<AccessibilityTimelineEntry> {
        val startOfDayMillis = TimelineClock.startOfTodayMillis()
        return loadEventsLocked().filter { it.timestampMillis >= startOfDayMillis }
    }

    @Synchronized
    fun clearBefore(cutoffMillis: Long) {
        val remainingEvents = loadEventsLocked().filter { it.timestampMillis >= cutoffMillis }
        saveEventsLocked(remainingEvents)
    }

    private fun loadEventsLocked(): List<AccessibilityTimelineEntry> {
        val raw = sharedPreferences.getString(KEY_EVENTS, null).orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val packageName = item.optString(KEY_PACKAGE_NAME)
                    val timestampMillis = item.optLong(KEY_TIMESTAMP_MILLIS)
                    if (packageName.isBlank() || timestampMillis <= 0L) {
                        continue
                    }
                    add(
                        AccessibilityTimelineEntry(
                            packageName = packageName,
                            timestampMillis = timestampMillis,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun saveEventsLocked(events: List<AccessibilityTimelineEntry>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject()
                    .put(KEY_PACKAGE_NAME, event.packageName)
                    .put(KEY_TIMESTAMP_MILLIS, event.timestampMillis),
            )
        }
        sharedPreferences.edit()
            .putString(KEY_EVENTS, array.toString())
            .apply()
    }

    private companion object {
        const val FILE_NAME = "accessibility_timeline_store"
        const val KEY_EVENTS = "events"
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_TIMESTAMP_MILLIS = "timestamp_millis"
        const val MAX_EVENTS = 600
        const val DUPLICATE_WINDOW_MILLIS = 1_500L
    }
}

data class AccessibilityTimelineEntry(
    val packageName: String,
    val timestampMillis: Long,
)
