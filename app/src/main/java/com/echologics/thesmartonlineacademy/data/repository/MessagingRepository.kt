package com.echologics.thesmartonlineacademy.data.repository

import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.data.model.Message
import com.echologics.thesmartonlineacademy.data.model.conversationId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.content.Context
import com.echologics.thesmartonlineacademy.utils.OneSignalHelper

class MessagingRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val conversationsCol = db.collection("conversations")

    private val authRepository = AuthRepository()

    private val oneSignalAppId: String by lazy {
        context.getString(context.resources.getIdentifier("onesignal_app_id", "string", context.packageName))
    }

    private val oneSignalRestKey: String by lazy {
        context.getString(context.resources.getIdentifier("onesignal_rest_api_key", "string", context.packageName))
    }

    // ── Send a message ────────────────────────────────────────────────────────

    suspend fun sendMessage(
        senderId: String,
        senderName: String,
        receiverId: String,
        receiverName: String,
        text: String
    ): Result<Unit> {
        return try {
            val convoId = conversationId(senderId, receiverId)
            val messageId = UUID.randomUUID().toString()

            val message = Message(
                id = messageId,
                conversationId = convoId,
                senderId = senderId,
                senderName = senderName,
                text = text,
                timestamp = System.currentTimeMillis()
            )

            val batch = db.batch()

            // Write message to subcollection
            val messageRef = conversationsCol
                .document(convoId)
                .collection("messages")
                .document(messageId)
            batch.set(messageRef, message)

            // Upsert conversation document
            val convoRef = conversationsCol.document(convoId)
            val existingConvo = convoRef.get().await()

            val currentUnread = if (existingConvo.exists()) {
                @Suppress("UNCHECKED_CAST")
                (existingConvo.get("unreadCount") as? Map<String, Long>)
                    ?.mapValues { it.value.toInt() } ?: emptyMap()
            } else emptyMap()

            // Increment receiver's unread count
            val newUnread = currentUnread.toMutableMap()
            newUnread[receiverId] = (newUnread[receiverId] ?: 0) + 1

            val conversation = Conversation(
                id = convoId,
                participantIds = listOf(senderId, receiverId),
                participantNames = mapOf(senderId to senderName, receiverId to receiverName),
                lastMessage = text,
                lastMessageAt = message.timestamp,
                lastSenderId = senderId,
                unreadCount = newUnread
            )
            batch.set(convoRef, conversation)

            batch.commit().await()

            val receiverPlayerId = authRepository.getPlayerIdForUser(receiverId)
            val (title, body) = OneSignalHelper.newMessagePayload(
                senderName = senderName,
                preview = text.take(80)
            )
            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey,
                appId = oneSignalAppId,
                playerId = receiverPlayerId,
                title = title,
                body = body,
                data = mapOf(
                    "type" to "new_message",
                    "conversationId" to convoId,
                    "senderId" to senderId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Mark all messages read for a user in a conversation ──────────────────

    suspend fun markRead(conversationId: String, userId: String): Result<Unit> {
        return try {
            conversationsCol.document(conversationId)
                .update("unreadCount.$userId", 0)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Real-time listeners ───────────────────────────────────────────────────

    fun listenToConversations(
        userId: String,
        onUpdate: (List<Conversation>) -> Unit
    ): ListenerRegistration {
        return conversationsCol
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val convos = snapshot?.documents?.mapNotNull {
                    it.toObject(Conversation::class.java)
                } ?: emptyList()
                onUpdate(convos)
            }
    }

    fun listenToMessages(
        conversationId: String,
        onUpdate: (List<Message>) -> Unit
    ): ListenerRegistration {
        return conversationsCol
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(Message::class.java)
                } ?: emptyList()
                onUpdate(messages)
            }
    }

    // ── Get conversation by ID ────────────────────────────────────────────────

    suspend fun getOrCreateConversation(
        myId: String,
        myName: String,
        otherId: String,
        otherName: String
    ): Result<Conversation> {
        return try {
            val convoId = conversationId(myId, otherId)
            val doc = conversationsCol.document(convoId).get().await()
            if (doc.exists()) {
                val convo = doc.toObject(Conversation::class.java)!!
                Result.success(convo)
            } else {
                val convo = Conversation(
                    id = convoId,
                    participantIds = listOf(myId, otherId),
                    participantNames = mapOf(myId to myName, otherId to otherName),
                    lastMessage = "",
                    lastMessageAt = System.currentTimeMillis()
                )
                conversationsCol.document(convoId).set(convo).await()
                Result.success(convo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}