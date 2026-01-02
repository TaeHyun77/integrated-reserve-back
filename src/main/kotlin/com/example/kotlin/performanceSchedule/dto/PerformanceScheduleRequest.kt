package com.example.kotlin.performanceSchedule.dto

import com.example.kotlin.performance.Performance
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.venue.Venue
import java.time.LocalDate
import java.time.LocalDateTime

data class PerformanceScheduleRequest(
    val venueId: Long,

    val performanceId: Long,

    val screeningDate: LocalDate,

    val startTime: LocalDateTime,

    val endTime: LocalDateTime
) {
    fun toScreen(venue: Venue, performance: Performance): PerformanceSchedule {
        return PerformanceSchedule(
            venue = venue,
            performance = performance,
            screeningDate = this.screeningDate,
            startTime = this.startTime,
            endTime = this.endTime
        )
    }
}