package com.snozzz.link.core.usage

import android.content.Context
import android.content.pm.PackageManager

class AppLabelResolver(context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private val appPackageName: String = context.packageName

    fun resolveAppName(packageName: String): String {
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

    fun shouldIgnoreTimelinePackage(packageName: String): Boolean {
        return packageName == appPackageName ||
            ignoredExactPackages.contains(packageName) ||
            ignoredPackagePrefixes.any(packageName::startsWith)
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

        val ignoredExactPackages = setOf(
            "android",
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.android.settings",
            "com.bbk.launcher2",
            "com.iqoo.powersaving",
            "com.vivo.doubleinstance",
            "com.vivo.xspace",
            "com.vivo.upslide",
            "com.vivo.daemonService",
        )

        val ignoredPackagePrefixes = listOf(
            "com.android.inputmethod",
            "com.sohu.inputmethod",
            "com.baidu.input",
            "com.google.android.inputmethod",
            "com.swiftkey",
            "com.android.launcher",
            "com.vivo.launcher",
            "com.vivo.permissionmanager",
        )
    }
}
