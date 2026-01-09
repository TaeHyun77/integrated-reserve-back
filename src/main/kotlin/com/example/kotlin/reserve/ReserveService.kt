package com.example.kotlin.reserve

import com.example.kotlin.config.Loggable
import com.example.kotlin.member.Member
import com.example.kotlin.performanceSchedule.PerformanceScheduleService
import com.example.kotlin.reserve.dto.Refund
import com.example.kotlin.reserve.dto.ReserveRequest
import com.example.kotlin.reserve.dto.ReserveResponse
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.seat.Seat
import com.example.kotlin.seat.repository.SeatRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReserveService(
    private val reserveRepository: ReserveRepository,
    private val seatRepository: SeatRepository,
    private val performanceScheduleService: PerformanceScheduleService
) : Loggable {

    @Transactional
    fun reserve(
        reserveRequest: ReserveRequest,
        member: Member
    ): ReserveResponse {

        // performanceSchedule 조회
        val performanceSchedule = performanceScheduleService.getPerformanceSchedule(reserveRequest.performanceScheduleId)

        // 예약하려는 좌석들의 유효성 검사 및 조회
        val seats = validateAndGetAvailableSeats(reserveRequest.reservedSeat, reserveRequest.performanceScheduleId)

        val totalAmount = performanceSchedule.performance.price * reserveRequest.reservedSeat.size // 원가
        val finalAmount = totalAmount - reserveRequest.rewardDiscountAmount // 총 가격

        if (finalAmount > member.credit) {
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_ENOUGH_CREDIT)
        }

        member.updateCreditAndReward(finalAmount, reserveRequest.rewardDiscountAmount)

        val reserve = Reserve(
            reservationNumber = reserveRequest.reservationNumber,
            reservedBy = reserveRequest.reservedBy,
            totalAmount = totalAmount,
            rewardDiscountAmount = reserveRequest.rewardDiscountAmount,
            finalAmount = finalAmount,
            reservedSeat = reserveRequest.reservedSeat,
            performanceScheduleId = performanceSchedule.id,
            member = member
        )

        reserveRepository.saveAndFlush(reserve)

        seats.forEach { seat ->
            seat.updateStatus(true, reserve)
        }

        log.info { "예약 성공 - ${member.username}" }

        return ReserveResponse.from(reserve)
    }

    fun validateAndGetAvailableSeats(
        seats: List<String>,
        performanceScheduleId: Long
    ): List<Seat> {
        return seats.map { seatNumber ->
            val seat = seatRepository.findByPerformanceScheduleIdAndSeatNumber(performanceScheduleId, seatNumber)
                ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_SEAT_INFO)

            if (seat.isReserved) {
                throw ReserveException(HttpStatus.CONFLICT, ErrorCode.SEAT_ALREADY_RESERVED)
            }
            seat
        }
    }

    @Transactional
    fun cancel(reserveNumber: String): Refund {

        val reserve = reserveRepository.findByReservationNumber(reserveNumber)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_RESERVE_INFO)

        val member = reserve.member

        member.credit += reserve.finalAmount
        member.reward += reserve.rewardDiscountAmount

        reserve.seatList.forEach {
            it.updateStatus(false, null)
        }

        reserveRepository.delete(reserve)

        return Refund(
            member.username,
            reserve.finalAmount,
            reserve.rewardDiscountAmount
        )
    }

    fun getUserReservations(username: String): List<ReserveResponse> {
        return reserveRepository.findByReservedBy(username)
            .map(ReserveResponse::from)
    }
}
