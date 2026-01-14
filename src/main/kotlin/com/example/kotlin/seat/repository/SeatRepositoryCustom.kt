package com.example.kotlin.seat.repository

import com.example.kotlin.seat.Seat
import com.example.kotlin.seat.dto.SeatResponse

interface SeatRepositoryCustom {

    fun findSeatByPerformanceScheduleId(performanceScheduleId: Long): List<Seat>

    fun findByPerformanceScheduleIdAndSeatNumber(performanceScheduleId: Long, seatNumber: String): Seat?
}