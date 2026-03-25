package com.snozzz.link.feature.setup

data class PermissionGuideUiState(
    val hasUsageAccess: Boolean = false,
    val isDismissed: Boolean = false,
) {
    val shouldShowGuide: Boolean
        get() = !hasUsageAccess && !isDismissed
}
