package com.echologics.thesmartonlineacademy.data.model

data class AdminStats(
    val totalTeachers: Int = 0,
    val pendingApprovals: Int = 0,
    val totalStudents: Int = 0,
    val totalBookings: Int = 0,
    val confirmedBookings: Int = 0,
    val pendingPayments: Int = 0,
    val completedSessions: Int = 0,
    val totalRevenue: Int = 0,
    val totalPlatformFee: Int = 0
)

data class AdminQrConfig(
    val qrImageUrl: String = "",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val updatedAt: Long = 0L
)
