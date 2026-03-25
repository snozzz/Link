package com.snozzz.link

import android.app.Application
import com.snozzz.link.core.security.SecureSessionStore

class LinkApplication : Application() {
    val sessionStore: SecureSessionStore by lazy {
        SecureSessionStore(this)
    }
}
