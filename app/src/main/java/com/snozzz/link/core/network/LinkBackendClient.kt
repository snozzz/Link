package com.snozzz.link.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class LinkBackendClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    suspend fun unlockPairCode(
        pairCode: String,
        nickname: String,
        devicePublicKey: String,
    ): UnlockInviteResult = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("pair_code", pairCode)
            .put("nickname", nickname)
            .put("device_public_key", devicePublicKey)
        val json = requestJson(
            path = "/v1/auth/pair-code/unlock",
            method = "POST",
            body = body,
        )
        UnlockInviteResult(
            sessionToken = json.getString("session_token"),
            pairId = json.getString("pair_id"),
            pairCode = json.getString("pair_code"),
            displayName = json.getString("display_name"),
            memberCount = json.getInt("member_count"),
        )
    }

    suspend fun fetchPairStatus(
        sessionToken: String,
        pairId: String,
    ): PairStatusResult = withContext(Dispatchers.IO) {
        val json = requestJson(
            path = "/v1/pair/status/$pairId",
            token = sessionToken,
        )
        PairStatusResult(
            pairId = json.getString("pair_id"),
            pairCode = json.getString("pair_code"),
            partnerNickname = json.getString("partner_nickname"),
            usageSharingEnabled = json.getBoolean("usage_sharing_enabled"),
            memberCount = json.getInt("member_count"),
        )
    }

    suspend fun syncMessages(
        sessionToken: String,
        pairId: String,
        outgoingMessages: List<OutgoingMessagePayload>,
    ): MessageSyncResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("pair_id", pairId)
            .put(
                "outgoing_messages",
                JSONArray().apply {
                    outgoingMessages.forEach { message ->
                        put(
                            JSONObject()
                                .put("local_id", message.localId)
                                .put("body", message.body)
                                .put("sent_at_epoch_millis", message.sentAtEpochMillis),
                        )
                    }
                },
            )
        val json = requestJson(
            path = "/v1/messages/sync",
            method = "POST",
            token = sessionToken,
            body = payload,
        )
        MessageSyncResult(
            acknowledgedMessageIds = json.getJSONArray("acknowledged_message_ids").toStringList(),
            messages = json.getJSONArray("messages").toBackendMessages(),
        )
    }

    suspend fun uploadUsage(
        sessionToken: String,
        pairId: String,
        capturedAtEpochMillis: Long,
        events: List<BackendUsageEvent>,
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("pair_id", pairId)
            .put("captured_at_epoch_millis", capturedAtEpochMillis)
            .put(
                "events",
                JSONArray().apply {
                    events.forEach { event ->
                        put(
                            JSONObject()
                                .put("app_name", event.appName)
                                .put("package_name", event.packageName)
                                .put("time_label", event.timeLabel)
                                .put("duration_label", event.durationLabel),
                        )
                    }
                },
            )
        requestJson(
            path = "/v1/usage/upload",
            method = "POST",
            token = sessionToken,
            body = payload,
        )
    }

    suspend fun fetchLatestUsage(
        sessionToken: String,
        pairId: String,
    ): LatestUsageResult? = withContext(Dispatchers.IO) {
        try {
            val json = requestJson(
                path = "/v1/usage/latest/$pairId",
                token = sessionToken,
            )
            LatestUsageResult(
                pairId = json.getString("pair_id"),
                ownerNickname = json.getString("owner_nickname"),
                capturedAtEpochMillis = json.getLong("captured_at_epoch_millis"),
                events = json.getJSONArray("events").toUsageEvents(),
            )
        } catch (exception: LinkBackendException) {
            if (exception.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                null
            } else {
                throw exception
            }
        }
    }

    suspend fun uploadPhoto(
        sessionToken: String,
        pairId: String,
        sourcePhotoId: String,
        capturedAtEpochMillis: Long,
        displayName: String,
        mimeType: String?,
        sizeBytes: Long,
        photoInputStream: InputStream,
    ): PhotoUploadResult = withContext(Dispatchers.IO) {
        val boundary = "LinkBoundary${System.currentTimeMillis()}"
        val connection = openConnection(
            path = "/v1/photos/upload",
            method = "POST",
            token = sessionToken,
            contentType = "multipart/form-data; boundary=$boundary",
        ).apply {
            doOutput = true
        }

        return@withContext try {
            DataOutputStream(connection.outputStream).use { output ->
                output.writeFormField(boundary, "pair_id", pairId)
                output.writeFormField(boundary, "source_photo_id", sourcePhotoId)
                output.writeFormField(boundary, "captured_at_epoch_millis", capturedAtEpochMillis.toString())
                output.writeFormField(boundary, "display_name", displayName)
                output.writeFormField(boundary, "mime_type", mimeType.orEmpty())
                output.writeFormField(boundary, "size_bytes", sizeBytes.toString())
                output.writeFileField(
                    boundary = boundary,
                    fieldName = "photo_file",
                    fileName = displayName,
                    contentType = mimeType ?: "application/octet-stream",
                    inputStream = photoInputStream,
                )
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }
            val statusCode = connection.responseCode
            val responseText = readStream(
                if (statusCode in 200..299) connection.inputStream else connection.errorStream,
            )
            if (statusCode !in 200..299) {
                val detail = responseText?.let(::parseErrorDetail)
                throw LinkBackendException(
                    statusCode = statusCode,
                    detail = detail,
                    message = detail ?: "HTTP $statusCode",
                )
            }
            val json = JSONObject(responseText ?: "{}")
            PhotoUploadResult(
                photoId = json.getString("photo_id"),
                stored = json.getBoolean("stored"),
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun fetchPhotoBackupSummary(
        sessionToken: String,
        pairId: String,
    ): PhotoBackupSummaryResult = withContext(Dispatchers.IO) {
        val json = requestJson(
            path = "/v1/photos/summary/$pairId",
            token = sessionToken,
        )
        PhotoBackupSummaryResult(
            pairId = json.getString("pair_id"),
            totalPhotos = json.getInt("total_photos"),
            myPhotoCount = json.getInt("my_photo_count"),
            latestUploadedAtEpochMillis = json.optLongOrNull("latest_uploaded_at_epoch_millis"),
            latestOwnerNickname = json.optStringOrNull("latest_owner_nickname"),
            latestDisplayName = json.optStringOrNull("latest_display_name"),
        )
    }

    private fun requestJson(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: JSONObject? = null,
    ): JSONObject {
        val connection = openConnection(
            path = path,
            method = method,
            token = token,
            contentType = if (body != null) "application/json" else null,
        )

        return try {
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val statusCode = connection.responseCode
            val responseText = readStream(
                if (statusCode in 200..299) connection.inputStream else connection.errorStream,
            )
            if (statusCode !in 200..299) {
                val detail = responseText?.let(::parseErrorDetail)
                throw LinkBackendException(
                    statusCode = statusCode,
                    detail = detail,
                    message = detail ?: "HTTP $statusCode",
                )
            }
            JSONObject(responseText ?: "{}")
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(
        path: String,
        method: String,
        token: String?,
        contentType: String?,
    ): HttpURLConnection {
        val url = URL(baseUrl.trimEnd('/') + path)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (contentType != null) {
                setRequestProperty("Content-Type", contentType)
            }
        }
    }

    private fun readStream(inputStream: InputStream?): String? {
        if (inputStream == null) return null
        return inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText().takeIf { it.isNotBlank() }
        }
    }

    private fun parseErrorDetail(responseText: String): String? {
        return runCatching { JSONObject(responseText).optString("detail") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    private fun JSONArray.toStringList(): List<String> {
        return List(length()) { index -> getString(index) }
    }

    private fun JSONArray.toBackendMessages(): List<BackendMessage> {
        return List(length()) { index ->
            val json = getJSONObject(index)
            BackendMessage(
                id = json.getString("id"),
                body = json.getString("body"),
                sentAtEpochMillis = json.getLong("sent_at_epoch_millis"),
                authorNickname = json.getString("author_nickname"),
                fromMe = json.getBoolean("from_me"),
            )
        }
    }

    private fun JSONArray.toUsageEvents(): List<BackendUsageEvent> {
        return List(length()) { index ->
            val json = getJSONObject(index)
            BackendUsageEvent(
                appName = json.getString("app_name"),
                packageName = json.getString("package_name"),
                timeLabel = json.getString("time_label"),
                durationLabel = json.optString("duration_label").takeIf { it.isNotBlank() },
            )
        }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        return if (isNull(key)) null else optLong(key)
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun DataOutputStream.writeFormField(boundary: String, name: String, value: String) {
        writeBytes("--$boundary\r\n")
        writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        write(value.toByteArray(Charsets.UTF_8))
        writeBytes("\r\n")
    }

    private fun DataOutputStream.writeFileField(
        boundary: String,
        fieldName: String,
        fileName: String,
        contentType: String,
        inputStream: InputStream,
    ) {
        writeBytes("--$boundary\r\n")
        writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        writeBytes("Content-Type: $contentType\r\n\r\n")
        inputStream.copyTo(this)
        writeBytes("\r\n")
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://36.151.149.108:8080"
    }
}

data class UnlockInviteResult(
    val sessionToken: String,
    val pairId: String,
    val pairCode: String,
    val displayName: String,
    val memberCount: Int,
)

data class PairStatusResult(
    val pairId: String,
    val pairCode: String,
    val partnerNickname: String,
    val usageSharingEnabled: Boolean,
    val memberCount: Int,
)

data class OutgoingMessagePayload(
    val localId: String,
    val body: String,
    val sentAtEpochMillis: Long,
)

data class BackendMessage(
    val id: String,
    val body: String,
    val sentAtEpochMillis: Long,
    val authorNickname: String,
    val fromMe: Boolean,
)

data class MessageSyncResult(
    val acknowledgedMessageIds: List<String>,
    val messages: List<BackendMessage>,
)

data class BackendUsageEvent(
    val appName: String,
    val packageName: String,
    val timeLabel: String,
    val durationLabel: String?,
)

data class LatestUsageResult(
    val pairId: String,
    val ownerNickname: String,
    val capturedAtEpochMillis: Long,
    val events: List<BackendUsageEvent>,
)

data class PhotoUploadResult(
    val photoId: String,
    val stored: Boolean,
)

data class PhotoBackupSummaryResult(
    val pairId: String,
    val totalPhotos: Int,
    val myPhotoCount: Int,
    val latestUploadedAtEpochMillis: Long?,
    val latestOwnerNickname: String?,
    val latestDisplayName: String?,
)

class LinkBackendException(
    val statusCode: Int,
    val detail: String?,
    message: String,
) : Exception(message)
