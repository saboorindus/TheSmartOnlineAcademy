package com.echologics.thesmartonlineacademy.data.repository

import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SessionRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun markSessionCompleted(bookingId: String): Result<Unit> {
        return try {
            db.collection("bookings").document(bookingId)
                .update(
                    mapOf(
                        "status" to BookingStatus.COMPLETED.name,
                        "completedAt" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendChatMessage(bookingId: String, message: ChatMessage): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            db.collection("bookings")
                .document(bookingId)
                .collection("chat")
                .document(id)
                .set(message.copy(id = id))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenToChat(
        bookingId: String,
        onMessages: (List<ChatMessage>) -> Unit
    ) = db.collection("bookings")
        .document(bookingId)
        .collection("chat")
        .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, _ ->
            val messages = snapshot?.documents?.mapNotNull {
                it.toObject(ChatMessage::class.java)
            } ?: emptyList()
            onMessages(messages)
        }
}