package com.echologics.thesmartonlineacademy.data.model

enum class UserRole { TEACHER, STUDENT, ADMIN }

data class User(
    val uid: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val onboardingComplete: Boolean = false
)