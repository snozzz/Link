package com.snozzz.link.core.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.snozzz.link.core.model.SessionSnapshot

class SecureSessionStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): SessionSnapshot? {
        val nickname = sharedPreferences.getString(KEY_NICKNAME, null) ?: return null
        val pairCode = sharedPreferences.getString(KEY_PAIR_CODE, null) ?: return null
        val sessionToken = sharedPreferences.getString(KEY_SESSION_TOKEN, null) ?: return null
        val pairId = sharedPreferences.getString(KEY_PAIR_ID, null) ?: return null
        return SessionSnapshot(
            nickname = nickname,
            pairCode = pairCode,
            sessionToken = sessionToken,
            pairId = pairId,
        )
    }

    fun save(snapshot: SessionSnapshot) {
        sharedPreferences.edit()
            .putString(KEY_NICKNAME, snapshot.nickname)
            .putString(KEY_PAIR_CODE, snapshot.pairCode)
            .putString(KEY_SESSION_TOKEN, snapshot.sessionToken)
            .putString(KEY_PAIR_ID, snapshot.pairId)
            .apply()
    }

    fun hasDismissedPermissionGuide(): Boolean {
        return sharedPreferences.getBoolean(KEY_PERMISSION_GUIDE_DISMISSED, false)
    }

    fun setPermissionGuideDismissed(value: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_PERMISSION_GUIDE_DISMISSED, value)
            .apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "secure_session_store"
        const val KEY_NICKNAME = "nickname"
        const val KEY_PAIR_CODE = "pair_code"
        const val KEY_SESSION_TOKEN = "session_token"
        const val KEY_PAIR_ID = "pair_id"
        const val KEY_PERMISSION_GUIDE_DISMISSED = "permission_guide_dismissed"
    }
}
