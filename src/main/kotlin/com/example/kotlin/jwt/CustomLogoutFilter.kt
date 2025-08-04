package com.example.kotlin.jwt

import com.example.kotlin.refresh.RefreshRepository
import io.jsonwebtoken.ExpiredJwtException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class CustomLogoutWebFilter(
    private val jwtUtil: JwtUtil,
    private val refreshRepository: RefreshRepository
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        // 로그아웃 경로가 아니면 필터 통과
        if (request.uri.path != "/logout" || request.method != HttpMethod.POST) {
            return chain.filter(exchange)
        }

        val refreshCookie = request.cookies.getFirst("refresh")?.value

        if (refreshCookie == null) {
            exchange.response.statusCode = HttpStatus.BAD_REQUEST
            return exchange.response.setComplete()
        }

        try {
            jwtUtil.isExpired(refreshCookie)
        } catch (e: ExpiredJwtException) {
            exchange.response.statusCode = HttpStatus.BAD_REQUEST
            return exchange.response.setComplete()
        }

        val category = jwtUtil.getCategory(refreshCookie)
        if (category != "refresh") {
            exchange.response.statusCode = HttpStatus.BAD_REQUEST
            return exchange.response.setComplete()
        }

        val exists = refreshRepository.existsByRefresh(refreshCookie)
        if (!exists) {
            response.statusCode = HttpStatus.BAD_REQUEST
            return response.setComplete()
        }

        refreshRepository.deleteByRefresh(refreshCookie)

        val expiredCookie = ResponseCookie.from("refresh", "")
            .path("/")
            .maxAge(0)
            .httpOnly(true)
            .build()

        response.addCookie(expiredCookie)
        response.statusCode = HttpStatus.OK

        return response.setComplete()
    }
}