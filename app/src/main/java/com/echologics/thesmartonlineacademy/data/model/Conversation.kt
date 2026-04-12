package com.echologics.thesmartonlineacademy.data.model

data class Conversation(
    val id: String = "",           // always sorted: "uid1_uid2" (smaller first)
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(), // uid -> name
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val lastSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap()  // uid -> unread count
)

data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// Helper to build a deterministic conversation ID from two user IDs
fun conversationId(uid1: String, uid2: String): String {
    return listOf(uid1, uid2).sorted().joinToString("_")
}