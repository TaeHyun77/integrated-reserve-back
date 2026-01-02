package com.example.kotlin.reserve.dto

import java.util.UUID

data class ReserveRequest (
    val reservationNumber: String = UUID.randomUUID().toString(),

    val reservedBy: String,

    val rewardDiscountAmount: Long,

    val reservedSeat: List<String>,

    val performanceScheduleId: Long,
)