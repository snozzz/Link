package com.snozzz.link.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snozzz.link.LinkApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as LinkApplication
    private val photoBackupRepository = app.photoBackupRepository

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh(autoSync = true)
    }

    fun refresh(autoSync: Boolean = false) {
        viewModelScope.launch {
            val hasPermission = photoBackupRepository.hasPermission()
            val summary = runCatching { photoBackupRepository.fetchSummary() }.getOrNull()
            _uiState.value = _uiState.value.copy(
                hasPhotoPermission = hasPermission,
                photoSummaryLine = when {
                    summary == null -> "服务器还没有图片备份记录"
                    summary.totalPhotos == 0 -> "服务器还没有图片备份记录"
                    else -> "服务器已保存 ${summary.totalPhotos} 张，最近一张是 ${summary.latestDisplayName ?: "未命名图片"}"
                },
                actionLabel = if (hasPermission) "立即备份下一批照片" else "开启相册备份",
            )
            if (autoSync && hasPermission) {
                syncPhotos()
            }
        }
    }

    fun onPhotoPermissionResult(granted: Boolean) {
        if (granted) {
            refresh(autoSync = true)
        } else {
            _uiState.value = _uiState.value.copy(
                hasPhotoPermission = false,
                photoBackupStatus = "你还没有开启相册权限，无法自动备份",
                actionLabel = "开启相册备份",
            )
        }
    }

    fun syncPhotos() {
        viewModelScope.launch {
            if (!photoBackupRepository.hasPermission()) {
                _uiState.value = _uiState.value.copy(
                    hasPhotoPermission = false,
                    photoBackupStatus = "需要先开启相册权限",
                    actionLabel = "开启相册备份",
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                hasPhotoPermission = true,
                isPhotoSyncing = true,
                photoBackupStatus = "正在扫描并上传相册图片…",
                actionLabel = "正在备份…",
            )
            val report = runCatching { photoBackupRepository.syncNextBatch() }.getOrElse {
                _uiState.value = _uiState.value.copy(
                    isPhotoSyncing = false,
                    photoBackupStatus = "相册备份失败，请稍后重试",
                    actionLabel = "立即备份下一批照片",
                )
                return@launch
            }
            val summary = runCatching { photoBackupRepository.fetchSummary() }.getOrNull()
            _uiState.value = _uiState.value.copy(
                hasPhotoPermission = true,
                isPhotoSyncing = false,
                photoBackupStatus = report.statusMessage,
                photoSummaryLine = when {
                    summary == null -> "服务器摘要读取失败，但本地同步已完成"
                    summary.totalPhotos == 0 -> "服务器还没有图片备份记录"
                    else -> "服务器已保存 ${summary.totalPhotos} 张，你这边已上传 ${summary.myPhotoCount} 张"
                },
                lastSyncedAtLabel = report.lastSyncedAtLabel,
                actionLabel = "立即备份下一批照片",
            )
        }
    }
}
