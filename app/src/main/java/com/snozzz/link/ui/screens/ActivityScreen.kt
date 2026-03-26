package com.snozzz.link.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snozzz.link.core.model.AppUsageSummaryItem
import com.snozzz.link.core.model.UsageTimelineEventItem
import com.snozzz.link.feature.activity.ActivityTimelineUiState
import com.snozzz.link.feature.activity.ActivityTimelineViewModel
import com.snozzz.link.ui.theme.Blush
import com.snozzz.link.ui.theme.ButterCream
import com.snozzz.link.ui.theme.LavenderMilk
import com.snozzz.link.ui.theme.MintCandy

@Composable
fun ActivityScreenRoute() {
    val viewModel: ActivityTimelineViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    ActivityScreen(
        uiState = uiState,
        onOpenUsageAccess = viewModel::openUsageSettings,
    )
}

@Composable
fun ActivityScreen(
    uiState: ActivityTimelineUiState,
    onOpenUsageAccess: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LavenderMilk.copy(alpha = 0.35f),
                        ButterCream.copy(alpha = 0.5f),
                        Color.White,
                    ),
                ),
            ),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "Moments",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "本地 Moments 会先在这里展示，再同步到服务器；如果对方已经上传，也会直接显示在下面。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
        }

        item {
            SyncStatusCard(
                title = "本地同步状态",
                body = uiState.localSyncMessage,
            )
        }

        item {
            PartnerMomentsCard(
                nickname = uiState.partnerNickname,
                refreshedAtLabel = uiState.partnerRefreshedAtLabel,
                status = uiState.partnerStatus,
                events = uiState.partnerEvents,
            )
        }

        if (!uiState.hasUsageAccess) {
            item {
                PermissionCard(onOpenUsageAccess = onOpenUsageAccess)
            }
        } else {
            item {
                TopAppsCard(topApps = uiState.topApps)
            }
            items(uiState.recentEvents) { item ->
                TimelineCard(item = item)
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
private fun PartnerMomentsCard(
    nickname: String,
    refreshedAtLabel: String,
    status: String,
    events: List<UsageTimelineEventItem>,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$nickname 的最新动态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            if (events.isNotEmpty()) {
                Text(
                    text = "同步时间 $refreshedAtLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MintCandy.copy(alpha = 0.95f),
                )
                events.take(6).forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${item.timeLabel} 打开 ${item.appName}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = item.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    onOpenUsageAccess: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "需要 Usage Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "这是 Android 的系统级授权。只有你手动打开后，App 才能统计当天前台应用和使用时长。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(onClick = onOpenUsageAccess) {
                Text(text = "打开权限设置")
            }
        }
    }
}

@Composable
private fun TopAppsCard(
    topApps: List<AppUsageSummaryItem>,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "今天最常使用的 App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (topApps.isEmpty()) {
                Text(
                    text = "今天暂时没有读取到前台使用数据。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            } else {
                topApps.forEachIndexed { index, app ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "${index + 1}. ${app.appName}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                            )
                        }
                        Text(
                            text = "${app.totalMinutes} 分钟",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Blush,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(
    item: UsageTimelineEventItem,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = item.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.timeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MintCandy.copy(alpha = 0.95f),
                )
            }
            Text(
                text = item.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Text(
                text = item.durationLabel?.let { "前台使用时长 $it" } ?: "已记录一次后台切换事件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}
