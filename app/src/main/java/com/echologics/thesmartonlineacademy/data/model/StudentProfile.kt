package com.echologics.thesmartonlineacademy.data.model

data class StudentProfile(
    val uid: String = "",
    val fullName: String = "",
    val subjects: List<String> = emptyList(),
    val level: String = "",
    val goal: String = "",
    val preferredLanguage: String = "",
    val timezone: String = "",
    val availabilityPrefs: List<String> = emptyList()
)