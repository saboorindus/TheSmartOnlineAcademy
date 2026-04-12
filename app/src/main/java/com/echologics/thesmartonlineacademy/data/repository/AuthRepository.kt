package com.echologics.thesmartonlineacademy.data.repository


import com.echologics.thesmartonlineacademy.data.model.StudentProfile
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUserId: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signUp(email: String, password: String, role: UserRole): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("UID missing"))
            val user = User(uid = uid, email = email, role = role)
            db.collection("users").document(uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("UID missing"))
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: return Result.failure(Exception("User not found"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logOut() = auth.signOut()

    suspend fun getCurrentUser(): Result<User> {
        val uid = currentUserId ?: return Result.failure(Exception("Not logged in"))
        return try {
            val doc = db.collection("users").document(uid).get().await()
            val user = doc.toObject(User::class.java) ?: return Result.failure(Exception("User not found"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveTeacherProfile(profile: TeacherProfile): Result<Unit> {
        return try {
            db.collection("teachers").document(profile.uid).set(profile).await()
            db.collection("users").document(profile.uid)
                .update("onboardingComplete", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveStudentProfile(profile: StudentProfile): Result<Unit> {
        return try {
            db.collection("students").document(profile.uid).set(profile).await()
            db.collection("users").document(profile.uid)
                .update("onboardingComplete", true).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}