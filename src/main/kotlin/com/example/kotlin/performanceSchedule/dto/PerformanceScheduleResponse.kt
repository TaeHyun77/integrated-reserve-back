package com.example.kotlin.performanceSchedule.dto

import com.example.kotlin.performance.dto.PerformanceResponse
import java.time.LocalDate
import java.time.LocalDateTime

data class PerformanceScheduleResponse(
    val id: Long? = null,

    val performance: PerformanceResponse,

    val screeningDate: LocalDate,

    val startTime: LocalDateTime,

    val endTime: LocalDateTime
)