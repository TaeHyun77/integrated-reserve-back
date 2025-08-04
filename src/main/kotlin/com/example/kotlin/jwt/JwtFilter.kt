package com.example.kotlin.jwt

import com.example.kotlin.member.MemberRepository
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger { }

@Component
class JwtFilter(
    private val jwtUtil: JwtUtil,
    private val memberRepository: MemberRepository
): WebFilter {

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain
    ): Mono<Void> {

        val request = exchange.request
        val accessToken = request.headers.getFirst("access")

        if (accessToken.isNullOrBlank()) {
            return chain.filter(exchange)
        }

        return try {
            if (jwtUtil.isExpired(accessToken)) {
                throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.ACCESSTOKEN_ISEXPIRED)
            }

        val category = jwtUtil.getCategory(accessToken)
        if (category != "access") {
            throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.IS_NOT_ACCESSTOKEN)
        }

        val username: String = jwtUtil.getUsername(accessToken)

        val member = memberRepository.findByUsername(username)
            ?: run {
                log.warn { "회원 정보를 찾을 수 없습니다. username: $username" }
                throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_MEMBER_INFO)
            }

        val customUserDetails = CustomUserDetails(member)
        val authToken = UsernamePasswordAuthenticationToken(
            customUserDetails, null, customUserDetails.authorities
        )

        SecurityContextHolder.getContext().authentication = authToken

        chain.filter(exchange)
    } catch (e: Exception) {
        throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TOKEN)
        }
    }
}