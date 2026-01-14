package com.example.kotlin.member

import com.example.kotlin.config.Loggable
import com.example.kotlin.member.dto.MemberRequest
import com.example.kotlin.member.dto.MemberResponse
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.util.parsingToken
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RequestMapping("/api/member")
@RestController
class MemberController(
    private val memberService: MemberService,
): Loggable {

    // 로그인 사용자 정보 조회
    @GetMapping("/info")
    fun memberInfo(request: HttpServletRequest): MemberResponse {

        val token = parsingToken(request)

        return memberService.memberInfo(token)
    }

    // 사용자 회원가입
    @PostMapping("/create")
    fun saveMember(@RequestBody memberRequest: MemberRequest) {
        return memberService.saveMember(memberRequest)
    }

    // 아이디 검증 로직
    @GetMapping("/check/validation/{username}")
    fun checkUsername(
        @PathVariable("username") username: String
    ): ResponseEntity<String> {
        log.info { "username 검증 : $username" }

        return memberService.checkUsername(username)
    }

    // 하루 한 번 리워드 지급 로직
    @PostMapping("/get/reward")
    fun earnRewardToday(
        request: HttpServletRequest,
    ): ResponseEntity<String> {

        val token: String = parsingToken(request)
        val today = LocalDate.now()

        val idempotencyKey: String = request.getHeader("Idempotency-key")
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_IN_HEADER_IDEMPOTENCY_KEY)

        log.info { "idempotencyKey : $idempotencyKey" }

        return memberService.earnRewardToday(token, today, idempotencyKey)
    }
}

