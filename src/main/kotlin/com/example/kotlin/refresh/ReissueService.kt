package com.example.kotlin.refresh

import com.example.kotlin.util.createCookie
import com.example.kotlin.jwt.JwtUtil
import io.jsonwebtoken.ExpiredJwtException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Service
class ReissueService(
    private val jwtUtil: JwtUtil,
    private val refreshRepository: RefreshRepository
) {

    @Transactional
    fun reToken(exchange: ServerWebExchange): Mono<Void> {
        val request = exchange.request
        val response = exchange.response

        val refreshToken = request.cookies.getFirst("refresh")?.value

        if (refreshToken.isNullOrBlank()) {
            response.statusCode = HttpStatus.BAD_REQUEST
            return response.setComplete()
        }

        try {
            jwtUtil.isExpired(refreshToken)
        } catch (e: ExpiredJwtException) {
            response.statusCode = HttpStatus.BAD_REQUEST
            return response.setComplete()
        }

        val category = jwtUtil.getCategory(refreshToken)
        if (category != "refresh") {
            response.statusCode = HttpStatus.BAD_REQUEST
            return response.setComplete()
        }

        val exists = refreshRepository.existsByRefresh(refreshToken)
        if (!exists) {
            response.statusCode = HttpStatus.BAD_REQUEST
            return response.setComplete()
        }

        val username = jwtUtil.getUsername(refreshToken)
        val role = jwtUtil.getRole(refreshToken)
        val name = jwtUtil.getName(refreshToken)
        val email = jwtUtil.getEmail(refreshToken)

        val newAccess = jwtUtil.createToken(
            username = username,
            name = name,
            email = email,
            role = role.name,
            category = "access",
            expired = 600_000L
        )

        val newRefresh = jwtUtil.createToken(
            username = username,
            name = name,
            email = email,
            role = role.name,
            category = "refresh",
            expired = 86_400_000L
        )

        refreshRepository.deleteByRefresh(refreshToken)
        createRefresh(username, newRefresh, 86_400_000L)

        response.headers.add("access", newAccess)
        response.addCookie(createCookie("refresh", newRefresh))
        response.statusCode = HttpStatus.OK

        return response.setComplete()
    }

    @Transactional
    fun createRefresh(username: String, refresh: String, expired: Long) {
        val refresh = Refresh(username = username, refresh = refresh, expiration = expired)

        refreshRepository.save(refresh)
    }
}