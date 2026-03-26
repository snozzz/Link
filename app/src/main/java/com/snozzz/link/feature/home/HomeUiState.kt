package com.snozzz.link.feature.home

data class HomeUiState(
    val hasPhotoPermission: Boolean = false,
    val isPhotoSyncing: Boolean = false,
    val photoBackupStatus: String = "相册备份尚未开始",
    val photoSummaryLine: String = "服务器还没有图片备份记录",
    val lastSyncedAtLabel: String = "--",
    val actionLabel: String = "开启相册备份",
)
