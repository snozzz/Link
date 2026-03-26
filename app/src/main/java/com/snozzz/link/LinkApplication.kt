package com.snozzz.link

import android.app.Application
import com.snozzz.link.core.chat.BackendChatRepository
import com.snozzz.link.core.network.LinkBackendClient
import com.snozzz.link.core.photo.PhotoBackupRepository
import com.snozzz.link.core.security.SecureSessionStore

class LinkApplication : Application() {
    val sessionStore: SecureSessionStore by lazy {
        SecureSessionStore(this)
    }

    val backendClient: LinkBackendClient by lazy {
        LinkBackendClient()
    }

    val chatRepository: BackendChatRepository by lazy {
        BackendChatRepository(
            backendClient = backendClient,
            sessionStore = sessionStore,
        )
    }

    val photoBackupRepository: PhotoBackupRepository by lazy {
        PhotoBackupRepository(
            context = this,
            backendClient = backendClient,
            sessionStore = sessionStore,
        )
    }
}
