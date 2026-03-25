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
                    text = "A tiny shared space for your day, your time, and your messages.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                )
            }
            item {
                HeroCard()
            }
            item {
                SectionCard(
                    title = "Today's Snapshot",
                    body = "Usage timeline, top apps, and sync health will land here in the next module.",
                    accent = Blush,
                )
            }
            item {
                SectionCard(
                    title = "Pair Space",
                    body = "Invite key login, pairing, and relationship state will live here.",
                    accent = MintCandy,
                )
            }
            item {
                SectionCard(
                    title = "Privacy Promise",
                    body = "The app will require explicit user consent before collecting usage access data.",
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
                        text = "Good evening, S + L",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "A soft dashboard for the two of you.",
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
                text = "This first build sets the navigation, theme, and screen structure so we can add auth, chat, and app-usage sync without reshaping the UI again.",
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
