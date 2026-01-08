package com.example.kotlin.member

import com.example.kotlin.config.Loggable
import com.example.kotlin.idempotency.IdempotencyService
import com.example.kotlin.jwt.JwtUtil
import com.example.kotlin.member.dto.MemberRequest
import com.example.kotlin.member.dto.MemberResponse
import com.example.kotlin.member.dto.MemberRewardResponse
import com.example.kotlin.member.dto.UsernameAvailabilityResponse
import com.example.kotlin.redis.lock.RedisLockUtil
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
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

    fun memberInfo(token: String): MemberResponse {
        val username = jwtUtil.getUsername(token)
        val member = getMemberByUsername(username)

        return MemberResponse.from(member)
    }

    @Transactional
    fun saveMember(memberRequest: MemberRequest) {
        if (memberRepository.existsByUsername(memberRequest.username)) {
            throw ReserveException(HttpStatus.CONFLICT, ErrorCode.DUPLICATED_USERNAME)
        }

        memberRepository.save(
            memberRequest.toEntity(passwordEncoder.encode(memberRequest.password))
        )
    }

    fun checkUsername(username: String): ResponseEntity<UsernameAvailabilityResponse> {
        val exists = memberRepository.existsByUsername(username)

        if (exists) {
            throw ReserveException(HttpStatus.CONFLICT, ErrorCode.DUPLICATED_USERNAME)
        }

        CheckUsername(username)

        return ResponseEntity.ok(
            UsernameAvailabilityResponse(true, "사용 가능한 아이디입니다.")
        )
    }

    fun earnRewardToday(
        token: String,
        today: LocalDate,
        idempotencyKey: String
    ): ResponseEntity<String> {

        val username = jwtUtil.getUsername(token)
        val member = getMemberByUsername(username)

        // username은 중복되지 않기 때문에 락 키로 사용 가능
        return redisLockUtil.acquireLockAndRun("${today}:${member.username}:todayReward") {
            idempotencyService.execute(
                idempotencyKey,
                "POST",
            ) {
                doEarnRewardToday(member, today)
            }
        }
    }

    @Transactional
    fun doEarnRewardToday(
        member: Member,
        today: LocalDate
    ): MemberRewardResponse {
        if (member.lastRewardDate == null || member.lastRewardDate != today) {
            member.lastRewardDate = today
            member.reward += 200

            log.info { "리워드 지급 성공 - $today ${member.username}님에게 리워드가 지급되었습니다." }
        } else {
            log.info {"리워드 지급 실패 - 날짜 : ${today}, 사용자: ${member.username}"}
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.REWARD_ALREADY_CLAIMED)
        }

        return MemberRewardResponse(member.username, member.reward, member.lastRewardDate)
    }

    fun getMemberByUsername(username: String): Member {
        return memberRepository.findByUsername(username)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_MEMBER_INFO)
    }
}