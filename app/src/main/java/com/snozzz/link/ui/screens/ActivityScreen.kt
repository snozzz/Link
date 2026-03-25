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
import androidx.compose.material3.OutlinedButton
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
        onOpenAccessibilitySettings = viewModel::openAccessibilitySettings,
    )
}

@Composable
fun ActivityScreen(
    uiState: ActivityTimelineUiState,
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
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
                text = when {
                    uiState.canReadMoments -> "本地时间线已刷新到 ${uiState.refreshedAtLabel}。${uiState.timelineSourceLabel}，后面接上服务端后会同步给对方。"
                    else -> "先给 Link 打开辅助功能或 Usage Access，才能读取今天看过哪些 App 和用了多久。"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
        }

        if (!uiState.canReadMoments) {
            item {
                PermissionCard(
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenUsageAccess = onOpenUsageAccess,
                )
            }
        } else {
            if (uiState.hasAccessibilityAccess) {
                item {
                    SourceCard(
                        title = "当前优先使用辅助功能时间线",
                        body = "这条数据源更适合记录“几点打开了什么 App”，对微信分身这类场景更友好。",
                    )
                }
            }
            if (uiState.hasUsageAccess) {
                item {
                    SourceCard(
                        title = "Usage Access 仍在补充时长统计",
                        body = "今天最常使用的 App 会优先用 Usage Access 的时长统计，没有时才回退到辅助功能的估算值。",
                    )
                }
            }
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
private fun PermissionCard(
    onOpenAccessibilitySettings: () -> Unit,
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
                text = "需要先开读取能力",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "如果你想稳定记录“几点打开了微信/抖音”，优先开辅助功能。若还想补充使用时长，再开 Usage Access。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Button(onClick = onOpenAccessibilitySettings) {
                Text(text = "打开辅助功能设置")
            }
            OutlinedButton(onClick = onOpenUsageAccess) {
                Text(text = "打开 Usage Access 设置")
            }
        }
    }
}

@Composable
private fun SourceCard(
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
                    text = "今天暂时没有读取到可用的前台数据。",
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
                text = item.durationLabel?.let { "前台停留约 $it" } ?: "已记录一次前台打开事件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}
