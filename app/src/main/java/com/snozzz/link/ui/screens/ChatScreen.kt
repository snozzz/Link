package com.snozzz.link.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snozzz.link.core.model.ChatMessage
import com.snozzz.link.core.model.MessageAuthor
import com.snozzz.link.core.model.MessageDeliveryState
import com.snozzz.link.feature.chat.ChatUiState
import com.snozzz.link.feature.chat.ChatViewModel
import com.snozzz.link.ui.theme.Blush
import com.snozzz.link.ui.theme.MintCandy
import com.snozzz.link.ui.theme.PeachSorbet

@Composable
fun ChatScreenRoute() {
    val viewModel: ChatViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        uiState = uiState,
        onDraftChange = viewModel::onDraftChange,
        onSendClick = viewModel::sendDraft,
    )
}

@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        Blush.copy(alpha = 0.28f),
                        MintCandy.copy(alpha = 0.32f),
                    ),
                ),
            )
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Messages",
            modifier = Modifier.padding(top = 24.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        StatusCard(status = uiState.partnerStatus)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }
        ComposerCard(
            draftMessage = uiState.draftMessage,
            onDraftChange = onDraftChange,
            onSendClick = onSendClick,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun StatusCard(status: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = PeachSorbet, shape = CircleShape),
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun ComposerCard(
    draftMessage: String,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = draftMessage,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                label = { Text("发一条消息") },
                maxLines = 4,
            )
            Button(
                onClick = onSendClick,
                enabled = draftMessage.isNotBlank(),
            ) {
                Text(text = "发送")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isMe = message.author == MessageAuthor.ME
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) Blush.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.95f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message.sentAtLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Text(
                        text = deliveryLabel(message.deliveryState),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isMe) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f) else MintCandy,
                    )
                }
            }
        }
    }
}

private fun deliveryLabel(state: MessageDeliveryState): String {
    return when (state) {
        MessageDeliveryState.LOCAL_ONLY -> "仅本地"
        MessageDeliveryState.SYNCING -> "同步中"
        MessageDeliveryState.SYNCED -> "已同步"
    }
}
