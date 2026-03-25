package com.snozzz.link.feature.setup

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.snozzz.link.ui.theme.Blush
import com.snozzz.link.ui.theme.ButterCream
import com.snozzz.link.ui.theme.MintCandy
import com.snozzz.link.ui.theme.PeachSorbet

@Composable
fun PermissionGuideScreen(
    uiState: PermissionGuideUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOpenAppSettingsList: () -> Unit,
    onSkipGuide: () -> Unit,
    onRefresh: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        onRefresh()
        onPauseOrDispose { }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PeachSorbet.copy(alpha = 0.42f),
                        ButterCream.copy(alpha = 0.58f),
                        MintCandy.copy(alpha = 0.52f),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderCard()
            PermissionCard(
                title = "1. 推荐先开辅助功能",
                body = "这一步是为了更稳定地记录“几点打开了什么 App”，尤其是微信分身这种系统隔离场景。进入后找到 Link，把辅助功能打开。",
                accent = Blush,
            )
            PermissionCard(
                title = "2. Usage Access 继续负责时长统计",
                body = "辅助功能更适合记前台切换时间线。Usage Access 适合补充“今天用了多久”这类统计，两者一起开效果最好。",
                accent = PeachSorbet,
            )
            PermissionCard(
                title = "3. 只记录必要信息",
                body = "Link 现在只会记录包名和时间戳，用来生成“某时间打开了某应用”的时间线，不会读取聊天内容或输入内容。",
                accent = MintCandy,
            )
            when {
                uiState.hasAccessibilityAccess && uiState.hasUsageAccess -> {
                    StateCard(
                        title = "辅助功能和 Usage Access 都已开启",
                        body = "Moments 会优先用辅助功能生成前台时间线，同时用 Usage Access 补充时长统计。",
                    )
                }
                uiState.hasAccessibilityAccess -> {
                    StateCard(
                        title = "辅助功能已开启",
                        body = "Moments 现在可以开始记录“某时间打开了某应用”。如果你还想要更稳的时长统计，可以继续把 Usage Access 也打开。",
                    )
                }
                else -> {
                    ActionCard(
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onOpenUsageAccess = onOpenUsageAccess,
                        onOpenAppDetails = onOpenAppDetails,
                        onOpenAppSettingsList = onOpenAppSettingsList,
                        onSkipGuide = onSkipGuide,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
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
                text = "Link 首次设置",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "这一步只做真正必要的配置，把你需要点的路径缩到最短。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    accent: Color,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = accent, shape = CircleShape),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
        }
    }
}

@Composable
private fun StateCard(
    title: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun ActionCard(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onOpenAppSettingsList: () -> Unit,
    onSkipGuide: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "直接去设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "优先点第一个按钮。进入辅助功能后找到 Link，把开关打开。Usage Access 可以放在第二步开。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(text = "打开辅助功能设置")
            }
            OutlinedButton(
                onClick = onOpenUsageAccess,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "打开 Usage Access 设置")
            }
            OutlinedButton(
                onClick = onOpenAppDetails,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "打开 Link 应用信息")
            }
            OutlinedButton(
                onClick = onOpenAppSettingsList,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "打开系统应用列表")
            }
            OutlinedButton(
                onClick = onSkipGuide,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "暂时跳过，先进入 App")
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "从设置返回时，Link 会自动重新检测权限状态。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun Bubble(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(color = color, shape = CircleShape),
    )
}
