package com.snozzz.link.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
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

    private fun requestJson(
        path: String,
        method: String = "GET",
        token: String? = null,
        body: JSONObject? = null,
    ): JSONObject {
        val url = URL(baseUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { output ->
                    output.write(body.toString().toByteArray(Charsets.UTF_8))
                }
            }
        }

        return try {
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

class LinkBackendException(
    val statusCode: Int,
    val detail: String?,
    message: String,
) : Exception(message)
