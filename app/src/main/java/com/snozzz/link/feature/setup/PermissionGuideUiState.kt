package com.snozzz.link.feature.setup

data class PermissionGuideUiState(
    val hasUsageAccess: Boolean = false,
    val hasAccessibilityAccess: Boolean = false,
    val isDismissed: Boolean = false,
) {
    val shouldShowGuide: Boolean
        get() = !hasAccessibilityAccess && !isDismissed
}
