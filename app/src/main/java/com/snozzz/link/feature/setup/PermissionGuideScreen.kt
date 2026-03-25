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
                title = "1. 先给 Link 开权限",
                body = "你不需要在设置里找抖音、微信、QQ。真正要打开的是 Link 自己的 Usage Access 开关，打开后 Link 才能读取这些应用的前台记录。",
                accent = Blush,
            )
            PermissionCard(
                title = "2. 为什么不能像相机那样弹窗",
                body = "因为 Usage Access 属于 Android 的特殊权限，不是普通运行时权限，所以系统不允许我直接弹一个一键授权框。",
                accent = PeachSorbet,
            )
            PermissionCard(
                title = "3. 对方动态怎么来",
                body = "双方都开启后，Link 会读取各自手机上的本地记录，再通过服务器同步，最后显示成“某时间打开了某应用”。",
                accent = MintCandy,
            )
            if (uiState.hasUsageAccess) {
                StateCard(
                    title = "权限已开启",
                    body = "已经检测到 Usage Access。返回主界面后，Moments 会开始读取本机当天的应用时间线。",
                )
            } else {
                ActionCard(
                    onOpenUsageAccess = onOpenUsageAccess,
                    onOpenAppDetails = onOpenAppDetails,
                    onOpenAppSettingsList = onOpenAppSettingsList,
                    onSkipGuide = onSkipGuide,
                )
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
                text = "优先点第一个按钮。进入后只需要把 Link 这一项打开，不用去找别的应用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(
                onClick = onOpenUsageAccess,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
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
