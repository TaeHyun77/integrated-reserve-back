package com.example.kotlin.seat

import com.example.kotlin.config.Loggable
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.performanceSchedule.repository.PerformanceScheduleRepository
import com.example.kotlin.reserve.Reserve
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.seat.dto.SeatResponse
import com.example.kotlin.seat.repository.SeatRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository
    ): Loggable {

    @Transactional
    fun initSeats(performanceScheduleId: Long) {

        val performanceSchedule: PerformanceSchedule = performanceScheduleRepository.findById(performanceScheduleId)
            .orElseThrow {
                throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_PERFORMANCE_SCHEDULE)
            }

        val seats = mutableListOf<Seat>()

        for (row in 'A'..'E') {
            for (col in 1..5) {
                val seatNumber = "$row$col"

                val seat = Seat(
                    seatNumber = seatNumber,
                    isReserved = false,
                    performanceSchedule = performanceSchedule
                )
                seats.add(seat)
            }
        }
        seatRepository.saveAll(seats)
    }

    /*
    * 특정 영화관에서 상영 중인 영화의 좌석 목록 조회
    * */
    fun getSeatList(performanceScheduleId: Long): List<SeatResponse> {
        val seats = seatRepository.findSeatByPerformanceScheduleId(performanceScheduleId)

        return seats.map(SeatResponse::from)
    }
}