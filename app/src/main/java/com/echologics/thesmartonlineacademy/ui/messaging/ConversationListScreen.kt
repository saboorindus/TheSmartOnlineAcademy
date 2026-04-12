package com.echologics.thesmartonlineacademy.ui.messaging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel,
    onConversationClick: (Conversation, String) -> Unit  // conversation, otherName
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Messages") })
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
            }

            uiState.conversations.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No messages yet", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Start a conversation from a teacher profile or booking",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = uiState.conversations,
                        key = { it.id }
                    ) { convo ->
                        ConversationRow(
                            conversation = convo,
                            otherName = viewModel.otherParticipantName(convo),
                            unreadCount = viewModel.unreadCount(convo),
                            onClick = {
                                onConversationClick(convo, viewModel.otherParticipantName(convo))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    otherName: String,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val hasUnread = unreadCount > 0

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasUnread)
                PurpleLight.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (hasUnread) Purple.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = PurpleLight,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = otherName.split(" ")
                            .take(2)
                            .joinToString("") { it.firstOrNull()?.uppercaseChar()?.toString() ?: "" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Purple
                    )
                }
            }

            // Name + last message
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherName,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = conversation.lastMessage.ifBlank { "No messages yet" },
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (hasUnread)
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Time + unread badge
            Column(horizontalAlignment = Alignment.End) {
                if (conversation.lastMessageAt > 0L) {
                    Text(
                        text = formatTime(conversation.lastMessageAt),
                        fontSize = 11.sp,
                        color = if (hasUnread) Purple
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
                if (hasUnread) {
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = CircleShape, color = Purple) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 11.sp,
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}