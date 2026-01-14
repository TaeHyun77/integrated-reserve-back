package com.example.kotlin.performanceSchedule.dto

import java.time.LocalDateTime

data class PerformanceScheduleListResponse(
    val venueId: Long? = null,

    val performanceId: Long? = null,

    val startTime: LocalDateTime
)