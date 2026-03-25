package com.snozzz.link.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snozzz.link.ui.theme.ButterCream
import com.snozzz.link.ui.theme.LavenderMilk
import com.snozzz.link.ui.theme.PeachSorbet

@Composable
fun ActivityScreen() {
    val sampleItems = listOf(
        "09:12 Opened WeChat",
        "13:40 Watched Bilibili for 26 min",
        "20:05 Reading mode placeholder",
    )

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
                text = "Foreground app events and daily usage summaries will be rendered here.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
        }
        items(sampleItems) { itemText ->
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
                    Text(
                        text = itemText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "These are sample cards only. Real data will come from UsageStats access and local aggregation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                    Text(
                        text = if (itemText.contains("26 min")) "Usage summary" else "Event item",
                        style = MaterialTheme.typography.labelLarge,
                        color = PeachSorbet,
                    )
                }
            }
        }
    }
}
