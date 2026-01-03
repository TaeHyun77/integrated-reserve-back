package com.example.kotlin.performanceSchedule.repository

import com.example.kotlin.screenInfo.QScreenInfo
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

class PerformanceScheduleRepositoryImpl(
    private val queryFactory: JPAQueryFactory
): PerformanceScheduleRepositoryCustom {

    override
    fun findPerformanceScheduleByVenueIdAndPerformanceId(venueId: Long?, performanceId: Long?): PerformanceScheduleResponse? {
        val screenInfo = QScreenInfo.screenInfo

        return queryFactory
            .select(
                Projections.constructor(
                PerformanceScheduleResponse::class.java,
                screenInfo.id,
                screenInfo.performance
            ))
            .from(screenInfo)
            .where(
                screenInfo.venue.id.eq(venueId),
                screenInfo.performance.id.eq(performanceId)
            )
            .fetchOne()
    }

    override
    fun findPerformanceScheduleListByVenueIdAndPerformanceId(
        venueId: Long?, performanceId: Long?
    ): List<PerformanceSchedule>? {
        val screenInfo = QScreenInfo.screenInfo

        return queryFactory
            .select(screenInfo)
            .from(screenInfo)
            .where(
                screenInfo.venue.id.eq(venueId),
                screenInfo.performance.id.eq(performanceId)
            )
            .fetch()
    }
}