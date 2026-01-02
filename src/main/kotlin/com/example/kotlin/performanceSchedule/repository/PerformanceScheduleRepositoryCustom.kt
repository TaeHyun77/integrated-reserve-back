package com.example.kotlin.performanceSchedule.repository

import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse

interface PerformanceScheduleRepositoryCustom {

    fun findScreenInfoByVenueIdAndPerformanceId(venueId: Long?, performanceId: Long?): PerformanceScheduleResponse?

    fun findScreenInfoListByVenueIdAndPerformanceId(venueId: Long?, performanceId: Long?): List<PerformanceSchedule>?

}