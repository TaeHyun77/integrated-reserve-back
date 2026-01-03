package com.example.kotlin.reserve

import com.example.kotlin.idempotency.IdempotencyService
import com.example.kotlin.config.Loggable
import com.example.kotlin.member.Member
import com.example.kotlin.member.MemberRepository
import com.example.kotlin.member.MemberService
import com.example.kotlin.redis.lock.RedisLockUtil
import com.example.kotlin.reserve.dto.ReserveRequest
import com.example.kotlin.reserve.dto.ReserveResponse
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import com.example.kotlin.performanceSchedule.PerformanceScheduleService
import com.example.kotlin.performanceSchedule.repository.PerformanceScheduleRepository
import com.example.kotlin.reserve.dto.Payment
import com.example.kotlin.reserve.dto.Refund
import com.example.kotlin.seat.Seat
import com.example.kotlin.seat.repository.SeatRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReserveService (
    private val redisLockUtil: RedisLockUtil,

    private val reserveRepository: ReserveRepository,
    private val memberRepository: MemberRepository,
    private val seatRepository: SeatRepository,

    private val idempotencyService: IdempotencyService,
    private val performanceScheduleService: PerformanceScheduleService,
    private val memberService: MemberService,
): Loggable {

    /*
    * 예약 로직 ( + 멱등성 )
    * */
    fun reserveSeat(
        reserveRequest: ReserveRequest,
        idempotencyKey: String
    ): ResponseEntity<String> {

        val member = memberService.getMemberByUsername(reserveRequest.reservedBy)

        // 예약하고자 하는 특정 screenInfoId의 각 좌석을 lock 키로 설정
        // 데드락 방지를 위해 정렬
        val lockKey: List<String> = reserveRequest.reservedSeat
            .sorted()
            .map { "lock:${reserveRequest.performanceScheduleId}:seat:${it}" }

        return redisLockUtil.acquireMultiLockAndRun(lockKey)
        { idempotencyService.execute(
            idempotencyKey,
            "POST",
        ) { doReserveSeats(reserveRequest, member) }
        }
    }

    @Transactional
    fun doReserveSeats(
        reserveRequest: ReserveRequest,
        member: Member
    ): ReserveResponse {
        try {
            // 공연 정보
            val performanceSchedule = performanceScheduleService.getPerformanceSchedule(reserveRequest.performanceScheduleId)

            // 예약하려는 좌석의 유효성 체크 ( 이미 예약되었는지 )
            val seats = getAvailableSeats(reserveRequest.performanceScheduleId, reserveRequest.reservedSeat, member)

            // 예약 금액 유효성 체크
            val payment = calculatePayableAmount(performanceSchedule, reserveRequest, member)

            // 예약 정보
            val reservation = createReservation(reserveRequest, member, performanceSchedule, payment)

            // 좌석 예약
            reserveSeats(seats, reservation)

            log.info {"예약 성공 - 사용자: ${member.username}, 좌석 수: ${reserveRequest.reservedSeat.size}, 총 가격: ${payment.totalAmount}, 포인트 사용: ${payment.rewardDiscountAmount}, 결제 금액: ${payment.finalAmount}"}

            return ReserveResponse.from(reservation)
        } catch (e: ReserveException) {
            log.info {"예약 실패 - 사용자: ${member.username}, ${e.errorCode}"}
            throw e
        }
    }

    // 예약하려는 좌석의 유효성 검증
    private fun getAvailableSeats(
        performanceScheduleId: Long,
        reservedSeat: List<String>,
        member: Member
    ): List<Seat> {
        return reservedSeat.map { seatNumber ->
            checkValidationSeat(performanceScheduleId, seatNumber, member)
        }
    }

    fun checkValidationSeat(
        performanceScheduleId: Long,
        seatNumber: String,
        member: Member
    ): Seat {
        val seat = seatRepository.findByScreenInfoAndSeatNumber(performanceScheduleId, seatNumber)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_SEAT_INFO)

        if (seat.isReserved == true) {
            log.info {"좌석 중복 예약 시도 - 사용자: ${member.username}, 좌석: $seatNumber"}

            throw ReserveException(HttpStatus.CONFLICT, ErrorCode.SEAT_ALREADY_RESERVED)
        }

        return seat
    }

    fun reserveSeats(
        seats: List<Seat>,
        reserve: Reserve
    ) {
        seats.forEach { seat ->

            // dirty checking
            seat.isReserved = true
            seat.reserve = reserve
        }
    }

    // 예약에 필요한 금액 검증
    fun calculatePayableAmount(
        performanceSchedule: PerformanceSchedule,
        reserveRequest: ReserveRequest,
        member: Member
    ): Payment {

        val totalAmount = performanceSchedule.performance.price * reserveRequest.reservedSeat.size
        val rewardDiscountAmount = reserveRequest.rewardDiscountAmount
        val finalAmount = totalAmount - rewardDiscountAmount

        if (finalAmount > member.credit) {
            log.info {"잔액 부족 - 사용자: ${member.username}, 보유: ${member.credit}"}

            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_ENOUGH_CREDIT)
        } else {
            member.updateCreditAndReward(finalAmount, rewardDiscountAmount)

            return Payment(totalAmount, rewardDiscountAmount, finalAmount)
        }
    }

    /*
    * 예약 취소 로직
    * */
    fun deleteReserveInfo(
        reserveNumber: String,
        idempotencyKey: String
    ): ResponseEntity<String> {

        val reserveInfo = reserveRepository.findByReservationNumber(reserveNumber)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_RESERVE_INFO)

        val member = memberRepository.findByUsername(reserveInfo.member.username)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_MEMBER_INFO)

        return redisLockUtil.acquireLockAndRun("${member.username}:${reserveNumber}:doDelete") {
            idempotencyService.execute(
                idempotencyKey,
                "DELETE"
            ) {
                doDeleteReserveInfo(reserveNumber, reserveInfo, member)
            }
        }
    }

    /*
    * 좌석 예약 취소 로직
    * */
    @Transactional
    fun doDeleteReserveInfo(
        reserveNumber: String,
        reserve: Reserve,
        member: Member
    ): Refund {
        try {
            // 사용자의 금액, 리워드 환불
            member.credit += reserve.finalAmount
            member.reward += reserve.rewardDiscountAmount
            memberRepository.save(member)

            // 좌석 상태 초기화
            reserve.reservedSeat.forEach { seatNumber ->
                val seat = seatRepository.findByScreenInfoAndSeatNumber(reserve.screenInfoId, seatNumber)
                    ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_SEAT_INFO)

                seat.is_reserved = false
                seat.reserve = null
                seatRepository.save(seat)
            }

            // 예약 정보 삭제
            reserveRepository.delete(reserve)

            log.info{"예약 취소 완료 - 예약번호: $reserveNumber, 사용자: ${member.username}, 환불 금액: ${reserve.finalAmount}, 리워드 환불: ${reserve.rewardDiscountAmount}"}
            return Refund(member.username, reserve.finalAmount, reserve.rewardDiscountAmount)
        } catch (e: ReserveException) {
            log.info { "예약 취소 실패 - 사용자: ${member.username}, 원인: ${e.errorCode}" }
            throw e
        }
    }

    private fun createReservation(
        request: ReserveRequest,
        member: Member,
        performanceSchedule: PerformanceSchedule,
        payment: Payment
    ): Reserve =
        reserveRepository.save(
            Reserve(
                reservationNumber = request.reservationNumber,
                reservedBy = request.reservedBy,
                totalAmount = payment.totalAmount,
                rewardDiscountAmount = payment.rewardDiscountAmount,
                finalAmount = payment.finalAmount,
                reservedSeat = request.reservedSeat,
                screenInfoId = performanceSchedule.id,
                member = member
            )
        )
}