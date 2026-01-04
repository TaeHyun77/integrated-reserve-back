package com.example.kotlin.performanceSchedule.repository

import com.example.kotlin.performanceSchedule.QPerformanceSchedule
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

class PerformanceScheduleRepositoryImpl(
    private val queryFactory: JPAQueryFactory
): PerformanceScheduleRepositoryCustom {

    override
    fun findPerformanceScheduleByVenueIdAndPerformanceId(venueId: Long?, performanceId: Long?): PerformanceScheduleResponse? {
        val performanceSchedule = QPerformanceSchedule.performanceSchedule

        return queryFactory
            .select(
                Projections.constructor(
                PerformanceScheduleResponse::class.java,
                    performanceSchedule.id,
                    performanceSchedule.performance
            ))
            .from(performanceSchedule)
            .where(
                performanceSchedule.venue.id.eq(venueId),
                performanceSchedule.performance.id.eq(performanceId)
            )
            .fetchOne()
    }

    override
    fun findPerformanceScheduleListByVenueIdAndPerformanceId(
        venueId: Long?, performanceId: Long?
    ): List<PerformanceSchedule>? {
        val screenInfo = QPerformanceSchedule.performanceSchedule

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