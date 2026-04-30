package com.echologics.thesmartonlineacademy.data.model

data class Withdrawal(
    val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val amount: Int = 0,
    val currency: String = "PKR",
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long = 0L,     // ✅ renamed
    val adminNote: String = "",
    val transactionId: String = "", // ✅ added
    val method: String = ""         // ✅ optional
)
 {
    fun displayAmount(): String = "$currency $amount"
}

enum class WithdrawalStatus {
    PENDING,    // Teacher requested, admin hasn't acted
    PAID,       // Admin clicked "Payment sent"
    REJECTED    // Admin rejected with a note
}



data class PlatformConfig(
    val platformFeePercent: Int = 10,   // e.g. 10 means 10%
    val minimumWithdrawal: Int = 500    // e.g. PKR 500
)