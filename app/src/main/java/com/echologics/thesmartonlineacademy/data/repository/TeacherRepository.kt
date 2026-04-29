package com.echologics.thesmartonlineacademy.data.repository

import com.echologics.thesmartonlineacademy.data.model.ApprovalStatus
import com.echologics.thesmartonlineacademy.data.model.Review
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class TeacherRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teachersCol = db.collection("teachers")
    private val reviewsCol = db.collection("reviews")

    suspend fun getApprovedTeachers(
        subject: String? = null,
        level: String? = null,
        maxRate: Int? = null
    ): Result<List<TeacherProfile>> {
        return try {
            var query: Query = teachersCol
                .whereEqualTo("approvalStatus", ApprovalStatus.APPROVED.name)

            val snapshot = query.get().await()
            var teachers = snapshot.documents.mapNotNull { it.toObject(TeacherProfile::class.java) }

            // Client-side filters (Firestore free tier avoids composite index requirements)
            if (!subject.isNullOrBlank()) {
                teachers = teachers.filter { it.subjects.any { s -> s.contains(subject, ignoreCase = true) } }
            }
            if (!level.isNullOrBlank()) {
                teachers = teachers.filter { it.levels.any { l -> l.contains(level, ignoreCase = true) } }
            }
            if (maxRate != null) {
                teachers = teachers.filter {
                    it.ratePerTenMin in 10..maxRate
                }
            }
            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeacherById(uid: String): Result<TeacherProfile> {
        return try {
            val doc = teachersCol.document(uid).get().await()
            val teacher = doc.toObject(TeacherProfile::class.java)
                ?: return Result.failure(Exception("Teacher not found"))
            Result.success(teacher)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReviewsForTeacher(teacherId: String): Result<List<Review>> {
        return try {
            val snapshot = reviewsCol
                .whereEqualTo("teacherId", teacherId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val reviews = snapshot.documents.mapNotNull { it.toObject(Review::class.java) }
            Result.success(reviews)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAverageRating(teacherId: String): Float {
        return try {
            val snapshot = reviewsCol.whereEqualTo("teacherId", teacherId).get().await()
            val ratings = snapshot.documents.mapNotNull { it.toObject(Review::class.java)?.rating }
            if (ratings.isEmpty()) 0f else ratings.average().toFloat()
        } catch (e: Exception) {
            0f
        }
    }
}