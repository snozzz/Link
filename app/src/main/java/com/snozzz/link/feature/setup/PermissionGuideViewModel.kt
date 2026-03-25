package com.snozzz.link.feature.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.snozzz.link.LinkApplication
import com.snozzz.link.core.accessibility.AccessibilityServiceController
import com.snozzz.link.core.security.SecureSessionStore
import com.snozzz.link.core.usage.UsageAccessController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PermissionGuideViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionStore: SecureSessionStore = (application as LinkApplication).sessionStore
    private val usageAccessController = UsageAccessController(application)
    private val accessibilityServiceController = AccessibilityServiceController(application)

    private val _uiState = MutableStateFlow(PermissionGuideUiState())
    val uiState: StateFlow<PermissionGuideUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val hasUsageAccess = usageAccessController.hasAccess()
        val hasAccessibilityAccess = accessibilityServiceController.isEnabled()
        if (hasAccessibilityAccess) {
            sessionStore.setPermissionGuideDismissed(true)
        }
        _uiState.value = PermissionGuideUiState(
            hasUsageAccess = hasUsageAccess,
            hasAccessibilityAccess = hasAccessibilityAccess,
            isDismissed = sessionStore.hasDismissedPermissionGuide(),
        )
    }

    fun openAccessibilitySettings() {
        accessibilityServiceController.openSettings()
    }

    fun openUsageSettings() {
        usageAccessController.openUsageSettings()
    }

    fun openAppDetails() {
        usageAccessController.openAppDetails()
    }

    fun openAppSettingsList() {
        usageAccessController.openAppSettingsList()
    }

    fun skipGuide() {
        sessionStore.setPermissionGuideDismissed(true)
        refresh()
    }
}
