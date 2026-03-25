package com.snozzz.link.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snozzz.link.feature.auth.InviteGateScreen
import com.snozzz.link.feature.auth.InviteGateViewModel
import com.snozzz.link.feature.setup.PermissionGuideScreen
import com.snozzz.link.feature.setup.PermissionGuideViewModel
import com.snozzz.link.ui.navigation.AuthenticatedNavHost
import com.snozzz.link.ui.theme.LinkTheme

@Composable
fun LinkApp() {
    LinkTheme {
        val inviteGateViewModel: InviteGateViewModel = viewModel()
        val authUiState by inviteGateViewModel.uiState.collectAsStateWithLifecycle()

        if (authUiState.isAuthenticated) {
            val permissionGuideViewModel: PermissionGuideViewModel = viewModel()
            val permissionUiState by permissionGuideViewModel.uiState.collectAsStateWithLifecycle()

            if (permissionUiState.shouldShowGuide) {
                PermissionGuideScreen(
                    uiState = permissionUiState,
                    onOpenUsageAccess = permissionGuideViewModel::openUsageSettings,
                    onOpenAppDetails = permissionGuideViewModel::openAppDetails,
                    onOpenAppSettingsList = permissionGuideViewModel::openAppSettingsList,
                    onSkipGuide = permissionGuideViewModel::skipGuide,
                    onRefresh = permissionGuideViewModel::refresh,
                )
            } else {
                AuthenticatedNavHost(
                    pairLabel = authUiState.pairLabel,
                    onLogout = inviteGateViewModel::logout,
                )
            }
        } else {
            InviteGateScreen(
                uiState = authUiState,
                onInviteKeyChange = inviteGateViewModel::onInviteKeyChange,
                onNicknameChange = inviteGateViewModel::onNicknameChange,
                onPairCodeChange = inviteGateViewModel::onPairCodeChange,
                onUnlockClick = inviteGateViewModel::unlockPrototype,
            )
        }
    }
}
