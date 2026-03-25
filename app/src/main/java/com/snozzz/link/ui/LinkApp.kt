package com.snozzz.link.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snozzz.link.feature.auth.InviteGateScreen
import com.snozzz.link.feature.auth.InviteGateViewModel
import com.snozzz.link.ui.navigation.AuthenticatedNavHost
import com.snozzz.link.ui.theme.LinkTheme

@Composable
fun LinkApp() {
    LinkTheme {
        val inviteGateViewModel: InviteGateViewModel = viewModel()
        val uiState by inviteGateViewModel.uiState.collectAsStateWithLifecycle()

        if (uiState.isAuthenticated) {
            AuthenticatedNavHost(
                pairLabel = uiState.pairLabel,
                onLogout = inviteGateViewModel::logout,
            )
        } else {
            InviteGateScreen(
                uiState = uiState,
                onInviteKeyChange = inviteGateViewModel::onInviteKeyChange,
                onNicknameChange = inviteGateViewModel::onNicknameChange,
                onPairCodeChange = inviteGateViewModel::onPairCodeChange,
                onUnlockClick = inviteGateViewModel::unlockPrototype,
            )
        }
    }
}
