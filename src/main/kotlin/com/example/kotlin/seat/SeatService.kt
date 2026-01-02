package com.example.kotlin.seat

import com.example.kotlin.config.Loggable
import com.example.kotlin.performance.repository.PerformanceRepository
import com.example.kotlin.performance.dto.PerformanceResponse
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.performanceSchedule.repository.PerformanceScheduleRepository
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse
import com.example.kotlin.seat.dto.SeatRequest
import com.example.kotlin.seat.dto.SeatResponse
import com.example.kotlin.seat.repository.SeatRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SeatService(
    private val seatRepository: SeatRepository,
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val performanceRepository: PerformanceRepository,
    ): Loggable {

    @Transactional
    fun initSeats(seatRequest: SeatRequest) {

        val performanceSchedule: PerformanceSchedule = performanceScheduleRepository.findById(seatRequest.screenInfoId)
            .orElseThrow { throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_SCREEN_INFO) }

        val seats = mutableListOf<Seat>()

        for (row in 'A'..'E') {
            for (col in 1..5) {
                val seatNumber = "$row$col"

                val seat = Seat(
                    seatNumber = seatNumber,
                    is_reserved = false,
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
    fun seatList(screenInfoId: Long): List<SeatResponse> {
        try {
            val seats = seatRepository.findSeatByScreenInfoId(screenInfoId)

            return seats.map {
                SeatResponse(
                    seatNumber = it.seatNumber,
                    is_reserved = it.is_reserved,
                    screenInfo = PerformanceScheduleResponse(
                        performance = PerformanceResponse(
                            type = it.performanceSchedule.performance.type,
                            title = it.performanceSchedule.performance.title,
                            duration = it.performanceSchedule.performance.duration,
                            price = it.performanceSchedule.performance.price
                        ),
                        screeningDate = it.performanceSchedule.screeningDate,
                        startTime = it.performanceSchedule.startTime,
                        endTime = it.performanceSchedule.endTime
                    )
                )
            }
        } catch(e: ReserveException) {
            log.info { "예약 좌석 리스트 반환 실패" }
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.FAIL_TO_RETURN_RESERVED_SEAT_LIST)
        }
    }

    fun seatPrice(performanceId: Long): Long {

        val performance = performanceRepository.findById(performanceId)
            .orElseThrow{throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_PERFORMANCE_INFO) }

        return performance.price
    }

    /*
    * 좌석 삭제
    * */
    @Transactional
    fun deleteSeat(seatId: Long) {

        val deleteSeat = seatRepository.findById(seatId)
            .orElseThrow { ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_SEAT_INFO) }

        seatRepository.delete(deleteSeat)
    }
}