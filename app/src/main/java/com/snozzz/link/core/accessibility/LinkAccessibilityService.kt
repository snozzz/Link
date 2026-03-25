package com.snozzz.link.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.snozzz.link.core.usage.AppLabelResolver

class LinkAccessibilityService : AccessibilityService() {
    private lateinit var store: AccessibilityTimelineStore
    private lateinit var labelResolver: AppLabelResolver
    private var lastRecordedPackage: String? = null
    private var lastRecordedTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = AccessibilityTimelineStore(applicationContext)
        labelResolver = AppLabelResolver(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        val packageName = event.packageName?.toString().orEmpty()
        if (packageName.isBlank() || labelResolver.shouldIgnoreTimelinePackage(packageName)) {
            return
        }
        val timestampMillis = event.eventTime.takeIf { it > 0L } ?: System.currentTimeMillis()
        if (packageName == lastRecordedPackage &&
            timestampMillis - lastRecordedTimestamp < 1_500L
        ) {
            return
        }
        val recorded = store.recordForegroundPackage(
            packageName = packageName,
            timestampMillis = timestampMillis,
        )
        if (recorded) {
            lastRecordedPackage = packageName
            lastRecordedTimestamp = timestampMillis
        }
    }

    override fun onInterrupt() = Unit
}
