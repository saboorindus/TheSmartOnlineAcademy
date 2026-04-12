package com.echologics.thesmartonlineacademy.data.model


enum class ApprovalStatus { PENDING, APPROVED, REJECTED }


data class TeacherProfile(
    val uid: String = "",
    val fullName: String = "",
    val country: String = "",
    val languages: List<String> = emptyList(),
    val bio: String = "",
    val subjects: List<String> = emptyList(),
    val levels: List<String> = emptyList(),
    val teachingStyles: List<String> = emptyList(),
    val yearsExperience: String = "",
    val education: String = "",
    val hourlyRate: String = "",
    val sessionLengths: List<String> = emptyList(),
    val availabilitySlots: Map<String, List<String>> = emptyMap(),
    val trialSessionEnabled: Boolean = false,
    val trialRate: String = "",
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val isVerified: Boolean = false
)