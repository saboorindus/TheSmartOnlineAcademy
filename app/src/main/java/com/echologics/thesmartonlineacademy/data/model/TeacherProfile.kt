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
    val availabilitySlots: Map<String, List<String>> = emptyMap(),
    val trialSessionEnabled: Boolean = false,
    val trialRate: String = "",
    val approvalStatus: ApprovalStatus = ApprovalStatus.PENDING,
    val isVerified: Boolean = false,

    val ratePerTenMin: Int = 0,
    val currency: String = "PKR",

    val disabled: Boolean = false

) {
    // Convenience display string shown on teacher cards
    fun displayRate(): String = "$currency $ratePerTenMin / 10 min"

    // Calculate total for a given duration in minutes
    fun totalFor(durationMinutes: Int): Int {
        val blocks = durationMinutes / 10
        return blocks * ratePerTenMin
    }

    fun totalDisplayFor(durationMinutes: Int): String = "$currency ${totalFor(durationMinutes)}"
}

data class TeacherWallet(
    val totalEarnings: Int = 0,      // lifetime earnings
    val availableBalance: Int = 0,   // can withdraw
    val withdrawn: Int = 0           // already paid out
)
