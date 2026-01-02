package com.example.kotlin.member

import com.example.kotlin.idempotency.IdempotencyService
import com.example.kotlin.config.Loggable
import com.example.kotlin.jwt.JwtUtil
import com.example.kotlin.member.dto.MemberRequest
import com.example.kotlin.member.dto.MemberResponse
import com.example.kotlin.redis.lock.RedisLockUtil
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.reserve.dto.ReserveResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil,
    private val idempotencyService: IdempotencyService,
    private val redisLockUtil: RedisLockUtil

): Loggable {

    fun memberInfo(
        token: String
    ): MemberResponse {

        val username = jwtUtil.getUsername(token)
        val member = getMemberByUsername(username)
        val memberInfo = MemberResponse.from(member)

        return memberInfo
    }

    @Transactional
    fun saveMember(
        memberRequest: MemberRequest
    ): Member {

        if (memberRepository.existsByUsername(memberRequest.username)) {
            throw ReserveException(
                HttpStatus.CONFLICT, ErrorCode.DUPLICATED_USERNAME
            )
        }

        val encodedPassword = passwordEncoder.encode(memberRequest.password)

        return memberRepository.save(
            memberRequest.toEntity(encodedPassword)
        )
    }


    fun checkUsername(username: CheckUsername): ResponseEntity<UsernameCheckResponse> {

        try {
            val exists = memberRepository.existsByUsername(username)

            return if (exists) {
                throw ReserveException(HttpStatus.CONFLICT, ErrorCode.DUPLICATED_USERNAME)
            } else {
                ResponseEntity.ok(
                    UsernameCheckResponse(true, "사용 가능한 아이디입니다.")
                )
            }
        } catch (e: ReserveException) {
            throw e
        }
    }

    fun earnRewardToday(token: String, today: LocalDate, idempotencyKey: String): ResponseEntity<String> {

        val username = jwtUtil.getUsername(token)

        val member = memberRepository.findByUsername(username)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_MEMBER_INFO)

        // username은 중복되지 않기 때문에 락 키로 사용 가능
        return redisLockUtil.acquireLockAndRun("${today}:${member.username}:earnReward") {
            idempotencyService.execute(
                key = idempotencyKey,
                url = "/member/reward",
                method = "POST",
            ) {
                doPayRewardToday(member, today)
            }
        }
    }

    @Transactional
    fun doPayRewardToday(member: Member, today: LocalDate): String {

        return try {
            if (member.last_reward_date == null || member.last_reward_date != today) {
                member.last_reward_date = today
                member.reward += 200
                memberRepository.save(member)
            } else {
                throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.REWARD_ALREADY_CLAIMED)
            }

            log.info { "리워드 지급 성공 - $today ${member.username}님에게 리워드가 지급되었습니다." }
            "이미 처리된 요청이거나, $today ${member.name}님에게 리워드가 지급되었습니다."
        } catch (e: ReserveException) {

            log.info {"리워드 지급 실패 - 날짜 : ${today}, 사용자: ${member.username}, 원인: ${e.errorCode}"}
            throw e
        }
    }

    fun getMemberByUsername(username: String): Member {
        return memberRepository.findByUsername(username)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_MEMBER_INFO)
    }
}

data class UsernameCheckResponse(
    val available: Boolean,
    val message: String
)