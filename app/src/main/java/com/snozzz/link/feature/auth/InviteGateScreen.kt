package com.snozzz.link.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.snozzz.link.ui.theme.Blush
import com.snozzz.link.ui.theme.ButterCream
import com.snozzz.link.ui.theme.MintCandy
import com.snozzz.link.ui.theme.PeachSorbet

@Composable
fun InviteGateScreen(
    uiState: InviteGateUiState,
    onInviteKeyChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPairCodeChange: (String) -> Unit,
    onUnlockClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PeachSorbet.copy(alpha = 0.45f),
                        ButterCream.copy(alpha = 0.55f),
                        MintCandy.copy(alpha = 0.48f),
                        Color.White,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            InviteHeader()
            SecurityCard()
            InviteForm(
                uiState = uiState,
                onInviteKeyChange = onInviteKeyChange,
                onNicknameChange = onNicknameChange,
                onPairCodeChange = onPairCodeChange,
                onUnlockClick = onUnlockClick,
            )
        }
    }
}

@Composable
private fun InviteHeader() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Bubble(color = PeachSorbet)
                Bubble(color = Blush)
                Bubble(color = MintCandy)
            }
            Text(
                text = "Link Gate",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "先用邀请码进入测试版，再和对方通过配对码建立私密空间。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun SecurityCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Security Prototype",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "当前版本已接入本地加密会话存储。下一步会由服务端签发邀请码、设备公钥和会话令牌。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun InviteForm(
    uiState: InviteGateUiState,
    onInviteKeyChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onPairCodeChange: (String) -> Unit,
    onUnlockClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Invite Access",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = onNicknameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("你的昵称") },
            )
            OutlinedTextField(
                value = uiState.pairCode,
                onValueChange = onPairCodeChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("配对码") },
                supportingText = { Text("例如 LOVE520 或 LINK01") },
            )
            OutlinedTextField(
                value = uiState.inviteKey,
                onValueChange = onInviteKeyChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("测试邀请码") },
                visualTransformation = PasswordVisualTransformation(),
            )
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onUnlockClick,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blush,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = "进入私密空间",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "这个阶段仍是原型校验。真正的邀请码验签、设备绑定和会话签发会在服务端模块完成。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
    }
}

@Composable
private fun Bubble(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .background(color = color, shape = CircleShape),
    )
}
