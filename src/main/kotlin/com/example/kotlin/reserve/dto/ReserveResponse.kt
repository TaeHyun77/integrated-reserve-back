package com.example.kotlin.reserve.dto

import com.example.kotlin.reserve.Reserve
import java.time.LocalDateTime

data class ReserveResponse (
    val reservationNumber: String,

    val reservedBy: String,

    val reservedSeat: List<String>,

    val totalAmount: Long,

    val rewardDiscountAmount: Long,

    val finalAmount: Long,

    val createdAt: LocalDateTime?
) {
    companion object {
        fun from(reserve: Reserve): ReserveResponse {
            return ReserveResponse(
                reservationNumber = reserve.reservationNumber,
                reservedBy = reserve.member.username,
                reservedSeat = reserve.reservedSeat,
                totalAmount = reserve.totalAmount,
                rewardDiscountAmount = reserve.rewardDiscountAmount,
                finalAmount = reserve.finalAmount,
                createdAt = reserve.createdAt
            )
        }
    }
}