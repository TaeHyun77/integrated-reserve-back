package com.example.kotlin.reserve

import com.example.kotlin.idempotency.IdempotencyService
import com.example.kotlin.member.MemberService
import com.example.kotlin.redis.lock.RedisLockUtil
import com.example.kotlin.reserve.dto.ReserveRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class ReserveFacadeService(
    private val redisLockUtil: RedisLockUtil,
    private val idempotencyService: IdempotencyService,
    private val memberService: MemberService,
    private val reserveService: ReserveService
) {

    fun reserveSeat(
        reserveRequest: ReserveRequest,
        idempotencyKey: String
    ): ResponseEntity<String> {

        val member = memberService.getMemberByUsername(reserveRequest.reservedBy)

        val lockKeys = reserveRequest.reservedSeat
            .sorted()
            .map { "lock:${reserveRequest.performanceScheduleId}:seat:$it" }

        return redisLockUtil.acquireMultiLockAndRun(lockKeys) {
            idempotencyService.execute(idempotencyKey, "POST") {
                reserveService.reserve(reserveRequest, member)
            }
        }
    }

    fun cancelReserve(
        reserveNumber: String,
        idempotencyKey: String
    ): ResponseEntity<String> {

        return redisLockUtil.acquireLockAndRun("reserve:$reserveNumber:cancel") {
            idempotencyService.execute(idempotencyKey, "DELETE") {
                reserveService.cancel(reserveNumber)
            }
        }
    }
}
