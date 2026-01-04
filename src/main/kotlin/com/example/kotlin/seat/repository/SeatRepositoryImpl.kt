package com.example.kotlin.seat.repository

import com.example.kotlin.seat.QSeat
import com.example.kotlin.seat.Seat
import com.querydsl.jpa.impl.JPAQueryFactory

class SeatRepositoryImpl(
    private val queryFactory: JPAQueryFactory
): SeatRepositoryCustom {

    override fun findSeatByScreenInfoId(performanceScheduleId: Long): List<Seat> {
        val seat = QSeat.seat

        return queryFactory
            .selectFrom(seat)
            .where(seat.performanceSchedule.id.eq(performanceScheduleId))
            .fetch()
    }

    override fun findByScreenInfoAndSeatNumber(performanceScheduleId: Long?, seatNumber: String): Seat? {
        val seat = QSeat.seat

        return queryFactory
            .selectFrom(seat)
            .where(
                seat.performanceSchedule.id.eq(performanceScheduleId),
                seat.seatNumber.eq(seatNumber)
            )
            .fetchOne()
    }
}