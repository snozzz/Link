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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snozzz.link.ui.theme.Blush
import com.snozzz.link.ui.theme.ButterCream
import com.snozzz.link.ui.theme.MintCandy
import com.snozzz.link.ui.theme.PeachSorbet

@Composable
fun PermissionGuideScreen(
    uiState: PermissionGuideUiState,
    onOpenUsageAccess: () -> Unit,
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
                title = "1. 打开 Usage Access",
                body = "这是查看“几点打开了什么应用、用了多久”必须的系统权限。Android 不允许应用内一键开启，所以只能跳转到系统页后手动打开。",
                accent = Blush,
            )
            PermissionCard(
                title = "2. 相册 / 文件权限",
                body = "当前版本并不依赖相册权限，所以我不会为了图省事在首启时乱申请。后面如果要发图片，再单独请求。",
                accent = PeachSorbet,
            )
            PermissionCard(
                title = "3. 对方动态同步",
                body = "要看到“对方于某时间打开某应用”，需要双方都开启 Usage Access，并把各自的本地记录同步到服务器后再互相拉取。",
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
                text = "这一步只做真正必要的配置，避免一上来申请一堆无关权限。",
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
                text = "现在去设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "点下面按钮后会进入系统页面。你只需要把 Link 的 Usage Access 开关打开，再返回 App 即可。",
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
                onClick = onSkipGuide,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "暂时跳过，先进入 App")
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "如果你现在跳过，后面在 Moments 页面仍然可以再打开这个权限。",
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
