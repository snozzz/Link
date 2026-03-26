package com.snozzz.link.core.photo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.snozzz.link.core.network.LinkBackendClient
import com.snozzz.link.core.network.PhotoBackupSummaryResult
import com.snozzz.link.core.security.SecureSessionStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhotoBackupRepository(
    private val context: Context,
    private val backendClient: LinkBackendClient,
    private val sessionStore: SecureSessionStore,
) {
    private val contentResolver = context.contentResolver
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    fun hasPermission(): Boolean {
        val permission = requiredPermission()
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    suspend fun syncNextBatch(batchSize: Int = 12): PhotoBackupSyncReport = withContext(Dispatchers.IO) {
        val session = sessionStore.load() ?: return@withContext PhotoBackupSyncReport(
            uploadedCount = 0,
            skippedCount = 0,
            pendingCount = 0,
            statusMessage = "请先登录后再备份相册",
        )
        if (!hasPermission()) {
            return@withContext PhotoBackupSyncReport(
                uploadedCount = 0,
                skippedCount = 0,
                pendingCount = 0,
                statusMessage = "需要先开启相册权限",
            )
        }

        val candidates = queryPendingPhotos(limit = batchSize)
        if (candidates.isEmpty()) {
            return@withContext PhotoBackupSyncReport(
                uploadedCount = 0,
                skippedCount = 0,
                pendingCount = 0,
                statusMessage = "当前没有待备份的新照片",
            )
        }

        var uploadedCount = 0
        var skippedCount = 0
        var markerTimestamp = loadLastSyncedTimestamp()
        var markerMediaId = loadLastSyncedMediaId()

        for (candidate in candidates) {
            val inputStream = contentResolver.openInputStream(candidate.uri)
            if (inputStream == null) {
                skippedCount += 1
                continue
            }
            inputStream.use { stream ->
                val result = backendClient.uploadPhoto(
                    sessionToken = session.sessionToken,
                    pairId = session.pairId,
                    sourcePhotoId = candidate.mediaId.toString(),
                    capturedAtEpochMillis = candidate.capturedAtEpochMillis,
                    displayName = candidate.displayName,
                    mimeType = candidate.mimeType,
                    sizeBytes = candidate.sizeBytes,
                    photoInputStream = stream,
                )
                if (result.stored) {
                    uploadedCount += 1
                } else {
                    skippedCount += 1
                }
                markerTimestamp = candidate.capturedAtEpochMillis
                markerMediaId = candidate.mediaId
                saveLastSyncedMarker(markerTimestamp, markerMediaId)
            }
        }

        PhotoBackupSyncReport(
            uploadedCount = uploadedCount,
            skippedCount = skippedCount,
            pendingCount = candidates.size,
            statusMessage = when {
                uploadedCount > 0 -> "已备份 $uploadedCount 张照片，继续打开 App 会自动上传下一批"
                else -> "这一批照片都已经在服务器上了"
            },
            lastSyncedAtLabel = formatTime(markerTimestamp),
        )
    }

    suspend fun fetchSummary(): PhotoBackupSummaryResult? = withContext(Dispatchers.IO) {
        val session = sessionStore.load() ?: return@withContext null
        backendClient.fetchPhotoBackupSummary(
            sessionToken = session.sessionToken,
            pairId = session.pairId,
        )
    }

    private fun queryPendingPhotos(limit: Int): List<LocalPhotoCandidate> {
        val results = mutableListOf<LocalPhotoCandidate>()
        val markerTimestamp = loadLastSyncedTimestamp()
        val markerMediaId = loadLastSyncedMediaId()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC, ${MediaStore.Images.Media._ID} ASC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val displayNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext() && results.size < limit) {
                val mediaId = cursor.getLong(idIndex)
                val displayName = cursor.getString(displayNameIndex) ?: "photo_$mediaId"
                val mimeType = cursor.getString(mimeTypeIndex)
                val sizeBytes = cursor.getLong(sizeIndex)
                val dateTakenMillis = cursor.getLong(dateTakenIndex)
                val dateAddedMillis = cursor.getLong(dateAddedIndex) * 1000L
                val capturedAtEpochMillis = maxOf(dateTakenMillis, dateAddedMillis)
                val isPending = capturedAtEpochMillis > markerTimestamp ||
                    (capturedAtEpochMillis == markerTimestamp && mediaId > markerMediaId)
                if (!isPending) {
                    continue
                }
                results += LocalPhotoCandidate(
                    mediaId = mediaId,
                    displayName = displayName,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    capturedAtEpochMillis = capturedAtEpochMillis,
                    uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId.toString()),
                )
            }
        }
        return results
    }

    private fun loadLastSyncedTimestamp(): Long {
        return preferences.getLong(KEY_LAST_SYNCED_TIMESTAMP, 0L)
    }

    private fun loadLastSyncedMediaId(): Long {
        return preferences.getLong(KEY_LAST_SYNCED_MEDIA_ID, -1L)
    }

    private fun saveLastSyncedMarker(timestamp: Long, mediaId: Long) {
        preferences.edit()
            .putLong(KEY_LAST_SYNCED_TIMESTAMP, timestamp)
            .putLong(KEY_LAST_SYNCED_MEDIA_ID, mediaId)
            .apply()
    }

    private fun formatTime(timestampMillis: Long): String {
        return if (timestampMillis <= 0L) {
            "--"
        } else {
            Instant.ofEpochMilli(timestampMillis)
                .atZone(zoneId)
                .format(timeFormatter)
        }
    }

    private data class LocalPhotoCandidate(
        val mediaId: Long,
        val displayName: String,
        val mimeType: String?,
        val sizeBytes: Long,
        val capturedAtEpochMillis: Long,
        val uri: Uri,
    )

    companion object {
        private const val PREFS_NAME = "photo_backup_store"
        private const val KEY_LAST_SYNCED_TIMESTAMP = "last_synced_timestamp"
        private const val KEY_LAST_SYNCED_MEDIA_ID = "last_synced_media_id"
    }
}

data class PhotoBackupSyncReport(
    val uploadedCount: Int,
    val skippedCount: Int,
    val pendingCount: Int,
    val statusMessage: String,
    val lastSyncedAtLabel: String = "--",
)
