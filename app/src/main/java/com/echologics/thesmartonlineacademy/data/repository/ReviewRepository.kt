package com.echologics.thesmartonlineacademy.data.repository

import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.Review
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ReviewRepository {

    private val db = FirebaseFirestore.getInstance()
    private val reviewsCol = db.collection("reviews")
    private val bookingsCol = db.collection("bookings")

    suspend fun submitReview(review: Review): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val reviewWithId = review.copy(id = id)

            val batch = db.batch()

            // Save review document
            batch.set(reviewsCol.document(id), reviewWithId)

            // Mark booking as reviewed so student can't review twice
            batch.update(
                bookingsCol.document(review.bookingId),
                mapOf(
                    "status" to BookingStatus.COMPLETED.name,
                    "reviewSubmitted" to true
                )
            )

            batch.commit().await()

            // Recalculate and store teacher average rating
            updateTeacherRating(review.teacherId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateTeacherRating(teacherId: String) {
        try {
            val snapshot = reviewsCol
                .whereEqualTo("teacherId", teacherId)
                .get().await()
            val ratings = snapshot.documents.mapNotNull {
                it.getDouble("rating")?.toFloat()
            }
            if (ratings.isEmpty()) return
            val avg = ratings.average().toFloat()
            val count = ratings.size
            db.collection("teachers").document(teacherId)
                .update(
                    mapOf(
                        "averageRating" to avg,
                        "reviewCount" to count
                    )
                ).await()
        } catch (_: Exception) {}
    }

    suspend fun hasReviewed(bookingId: String): Boolean {
        return try {
            val snapshot = reviewsCol
                .whereEqualTo("bookingId", bookingId)
                .get().await()
            !snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
}