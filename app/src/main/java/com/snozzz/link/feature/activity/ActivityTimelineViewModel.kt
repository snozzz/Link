package com.snozzz.link.feature.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.snozzz.link.core.accessibility.AccessibilityServiceController
import com.snozzz.link.core.accessibility.AccessibilityTimelineRepository
import com.snozzz.link.core.model.UsageTimelineSnapshot
import com.snozzz.link.core.usage.UsageAccessController
import com.snozzz.link.core.usage.UsageTimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityTimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val usageAccessController = UsageAccessController(application)
    private val accessibilityServiceController = AccessibilityServiceController(application)
    private val usageTimelineRepository = UsageTimelineRepository(application)
    private val accessibilityTimelineRepository = AccessibilityTimelineRepository(application)

    private val _uiState = MutableStateFlow(ActivityTimelineUiState())
    val uiState: StateFlow<ActivityTimelineUiState> = _uiState.asStateFlow()

    fun refresh() {
        val hasUsageAccess = usageAccessController.hasAccess()
        val hasAccessibilityAccess = accessibilityServiceController.isEnabled()
        if (!hasUsageAccess && !hasAccessibilityAccess) {
            _uiState.value = ActivityTimelineUiState()
            return
        }

        val usageSnapshot: UsageTimelineSnapshot? = if (hasUsageAccess) {
            usageTimelineRepository.loadTodaySnapshot()
        } else {
            null
        }
        val accessibilitySnapshot: UsageTimelineSnapshot? = if (hasAccessibilityAccess) {
            accessibilityTimelineRepository.loadTodaySnapshot()
        } else {
            null
        }

        _uiState.value = ActivityTimelineUiState(
            hasUsageAccess = hasUsageAccess,
            hasAccessibilityAccess = hasAccessibilityAccess,
            timelineSourceLabel = when {
                hasAccessibilityAccess -> "前台时间线来自辅助功能"
                hasUsageAccess -> "当前只用了 Usage Access，分身应用可能漏记"
                else -> "未开启前台记录"
            },
            topApps = when {
                !usageSnapshot?.topApps.isNullOrEmpty() -> usageSnapshot!!.topApps
                else -> accessibilitySnapshot?.topApps.orEmpty()
            },
            recentEvents = when {
                !accessibilitySnapshot?.recentEvents.isNullOrEmpty() -> accessibilitySnapshot!!.recentEvents
                else -> usageSnapshot?.recentEvents.orEmpty()
            },
            refreshedAtLabel = accessibilitySnapshot?.refreshedAtLabel
                ?: usageSnapshot?.refreshedAtLabel
                ?: "--:--",
        )
    }

    fun openUsageSettings() {
        usageAccessController.openUsageSettings()
    }

    fun openAccessibilitySettings() {
        accessibilityServiceController.openSettings()
    }
}
