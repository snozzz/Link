package com.snozzz.link.core.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

class AccessibilityServiceController(
    private val context: Context,
) {
    fun isEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1
        if (!enabled) {
            return false
        }
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val serviceId = ComponentName(context, LinkAccessibilityService::class.java).flattenToString()
        return enabledServices.split(':').any { it.equals(serviceId, ignoreCase = true) }
    }

    fun openSettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
