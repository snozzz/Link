package com.snozzz.link.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snozzz.link.ui.theme.Blush
import com.snozzz.link.ui.theme.ButterCream
import com.snozzz.link.ui.theme.MintCandy
import com.snozzz.link.ui.theme.PeachSorbet

@Composable
fun HomeScreen() {
    val floatTransition = rememberInfiniteTransition(label = "float")
    val bubbleAlpha = floatTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bubbleAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PeachSorbet.copy(alpha = 0.45f),
                        ButterCream.copy(alpha = 0.65f),
                        Color.White,
                    ),
                ),
            ),
    ) {
        FloatingDecor(alpha = bubbleAlpha.value)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = "Link",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "把你们每天的时间线、留言和状态放进一个轻一点的小空间里。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }
            item {
                HeroCard()
            }
            item {
                SectionCard(
                    title = "今日概览",
                    body = "Moments 会汇总今天使用过的 App、最近的时间线，以及后续的同步状态。",
                    accent = Blush,
                )
            }
            item {
                SectionCard(
                    title = "配对空间",
                    body = "邀请码登录、配对码绑定和双方关系状态会放在这一层。",
                    accent = MintCandy,
                )
            }
            item {
                SectionCard(
                    title = "同步说明",
                    body = "如果要看到对方于某时间打开某应用，双方都需要开启 Usage Access，并通过服务器交换各自的本地记录。",
                    accent = PeachSorbet,
                )
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "今晚的共享空间",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "先把本地能力打稳，再把双方同步接上去。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PastelDot(color = PeachSorbet)
                    PastelDot(color = Blush)
                    PastelDot(color = MintCandy)
                }
            }
            Text(
                text = "现在这版已经有登录门禁、本地聊天和 Usage 时间线读取。接下来会把邀请码校验、配对状态和服务器同步真正接起来。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun FloatingDecor(alpha: Float) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .padding(start = 28.dp, top = 120.dp)
                .size(82.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(Blush.copy(alpha = 0.3f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 88.dp, end = 30.dp)
                .size(54.dp)
                .alpha(alpha * 0.8f)
                .clip(CircleShape)
                .background(MintCandy.copy(alpha = 0.42f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(24.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(PeachSorbet.copy(alpha = 0.28f)),
        )
    }
}

@Composable
private fun SectionCard(
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent),
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun PastelDot(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color),
    )
}
