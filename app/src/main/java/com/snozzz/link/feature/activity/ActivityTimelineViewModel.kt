package com.snozzz.link.feature.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.snozzz.link.core.model.UsageTimelineSnapshot
import com.snozzz.link.core.usage.UsageAccessController
import com.snozzz.link.core.usage.UsageTimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityTimelineViewModel(application: Application) : AndroidViewModel(application) {
    private val usageAccessController = UsageAccessController(application)
    private val usageTimelineRepository = UsageTimelineRepository(application)

    private val _uiState = MutableStateFlow(ActivityTimelineUiState())
    val uiState: StateFlow<ActivityTimelineUiState> = _uiState.asStateFlow()

    fun refresh() {
        if (!usageAccessController.hasAccess()) {
            _uiState.value = ActivityTimelineUiState(hasUsageAccess = false)
            return
        }

        val snapshot: UsageTimelineSnapshot = usageTimelineRepository.loadTodaySnapshot()
        _uiState.value = ActivityTimelineUiState(
            hasUsageAccess = true,
            topApps = snapshot.topApps,
            recentEvents = snapshot.recentEvents,
            refreshedAtLabel = snapshot.refreshedAtLabel,
        )
    }

    fun openUsageSettings() {
        usageAccessController.openUsageSettings()
    }
}
