package com.example.kotlin.seat.dto

import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse

data class SeatResponse(
    val seatNumber: String?,

    val is_reserved: Boolean?,

    val screenInfo: PerformanceScheduleResponse
)