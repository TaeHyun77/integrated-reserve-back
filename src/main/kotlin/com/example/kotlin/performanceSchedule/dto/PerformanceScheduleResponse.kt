package com.example.kotlin.performanceSchedule.dto

import com.example.kotlin.performance.dto.PerformanceResponse
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import java.time.LocalDate
import java.time.LocalDateTime

data class PerformanceScheduleResponse(
    val id: Long? = null,

    val performance: PerformanceResponse,

    val screeningDate: LocalDate,

    val startTime: LocalDateTime,

    val endTime: LocalDateTime
) {
    companion object {
        fun from(performanceSchedule: PerformanceSchedule): PerformanceScheduleResponse {
            return PerformanceScheduleResponse(
                id = performanceSchedule.id,
                performance = PerformanceResponse(
                    id = performanceSchedule.performance.id,
                    type = performanceSchedule.performance.type,
                    title = performanceSchedule.performance.title,
                    duration = performanceSchedule.performance.duration,
                    price = performanceSchedule.performance.price
                ),
                screeningDate = performanceSchedule.screeningDate,
                startTime = performanceSchedule.startTime,
                endTime = performanceSchedule.endTime
            )
        }
    }
}