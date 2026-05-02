package com.echologics.thesmartonlineacademy.data.repository

import com.echologics.thesmartonlineacademy.data.model.AdminStats
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.ChatMessage
import com.echologics.thesmartonlineacademy.data.model.DrawPath
import com.echologics.thesmartonlineacademy.data.model.TeacherWallet
import com.echologics.thesmartonlineacademy.data.model.toDrawPath
import com.echologics.thesmartonlineacademy.data.model.toMap
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class SessionRepository {

    private val db = FirebaseFirestore.getInstance()

//    suspend fun markSessionCompleted(bookingId: String): Result<Unit> {
//        return try {
//            db.collection("bookings").document(bookingId)
//                .update(
//                    mapOf(
//                        "status" to BookingStatus.COMPLETED.name,
//                        "completedAt" to System.currentTimeMillis()
//                    )
//                ).await()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    suspend fun markSessionCompleted(bookingId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->

                val bookingRef = db.collection("bookings").document(bookingId)
                val bookingSnap = transaction.get(bookingRef)

                val booking = bookingSnap.toObject(Booking::class.java)
                    ?: throw Exception("Booking not found")

                // Prevent double completion
                if (booking.status == BookingStatus.COMPLETED) {
                    return@runTransaction
                }

                val teacherWalletRef =
                    db.collection("teacherWallets").document(booking.teacherId)

                val walletSnap = transaction.get(teacherWalletRef)
                val wallet = walletSnap.toObject(TeacherWallet::class.java) ?: TeacherWallet()

                val updatedWallet = wallet.copy(
                    totalEarnings = wallet.totalEarnings + booking.teacherNetEarning,
                    availableBalance = wallet.availableBalance + booking.teacherNetEarning
                )

                val adminRef = db.collection("adminStats").document("global")
                val adminSnap = transaction.get(adminRef)
                val admin = adminSnap.toObject(AdminStats::class.java) ?: AdminStats()

                val updatedAdmin = admin.copy(
                    totalBookings = admin.totalBookings + 1,
                    completedSessions = admin.completedSessions + 1,
                    totalRevenue = admin.totalRevenue + booking.totalAmount,
                    totalPlatformFee = admin.totalPlatformFee + booking.platformFeeAmount
                )


                // 🔹 Update booking
                transaction.update(
                    bookingRef,
                    mapOf(
                        "status" to BookingStatus.COMPLETED.name,
                        "completedAt" to System.currentTimeMillis()
                    )
                )

                // 🔹 Update wallet
                transaction.set(teacherWalletRef, updatedWallet)

                // 🔹 Update admin stats
                transaction.set(adminRef, updatedAdmin)

            }.await()

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


    fun raiseHand(bookingId: String, studentId: String) {
        db.collection("bookings").document(bookingId)
            .update("raisedHands", FieldValue.arrayUnion(studentId))
    }

    fun lowerHand(bookingId: String, studentId: String) {
        db.collection("bookings").document(bookingId)
            .update("raisedHands", FieldValue.arrayRemove(studentId))
    }

    fun listenToRaisedHands(
        bookingId: String,
        onUpdate: (List<String>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("bookings").document(bookingId)
            .addSnapshotListener { snap, _ ->
                @Suppress("UNCHECKED_CAST")
                val hands = snap?.get("raisedHands") as? List<String> ?: emptyList()
                onUpdate(hands)
            }
    }


    fun sendStroke(bookingId: String, path: DrawPath) {
        db.collection("bookings")
            .document(bookingId)
            .collection("strokes")
            .document(path.id)
            .set(path.toMap())
    }

    fun listenToStrokes(
        bookingId: String,
        onUpdate: (List<DrawPath>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return db.collection("bookings")
            .document(bookingId)
            .collection("strokes")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val paths = snap?.documents?.mapNotNull { doc ->
                    doc.data?.toDrawPath()
                } ?: emptyList()
                onUpdate(paths)
            }
    }

    fun clearStrokes(bookingId: String) {
        db.collection("bookings")
            .document(bookingId)
            .collection("strokes")
            .get()
            .addOnSuccessListener { snap ->
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit()
            }
    }


}