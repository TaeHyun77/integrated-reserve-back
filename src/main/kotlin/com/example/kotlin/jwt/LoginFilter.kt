package com.example.kotlin.jwt

import com.example.kotlin.config.Loggable
import com.example.kotlin.util.createCookie
import com.example.kotlin.refresh.Refresh
import com.example.kotlin.refresh.RefreshRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Component
class LoginFilter(
    @Qualifier("reactiveAuthenticationManager")
    private val authenticationManager: ReactiveAuthenticationManager,

    private val jwtUtil: JwtUtil,
    private val refreshRepository: RefreshRepository
) : WebFilter, Loggable {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request

        // 로그인 요청이 아닌 경우 필터 통과
        if (request.uri.path != "/login" || request.method != HttpMethod.POST) {
            return chain.filter(exchange)
        }

        return request.body
            .map { dataBuffer ->
                val byteBuffer = dataBuffer.asByteBuffer()
                val bytes = ByteArray(byteBuffer.remaining())
                byteBuffer.get(bytes)
                String(bytes, Charsets.UTF_8)
            }
            .reduce { acc, next -> acc + next }
            .flatMap { body ->
                val mapper = jacksonObjectMapper()
                val loginDto = mapper.readValue(body, LoginRequest::class.java)

                val authToken = UsernamePasswordAuthenticationToken(
                    loginDto.username, loginDto.password
                )

                authenticationManager.authenticate(authToken)
                    .flatMap { authentication ->
                        val userDetails = authentication.principal as CustomUserDetails
                        val username = userDetails.username
                        val name = userDetails.getName()
                        val email = userDetails.getEmail()
                        val role = userDetails.authorities.first().authority

                        val access = jwtUtil.createToken(username, name, email, role, "access", 30 * 60 * 1000)
                        val refresh = jwtUtil.createToken(username, name, email, role, "refresh", 60 * 60 * 1000)

                        log.info { "Access Token: $access" }
                        log.info { "Refresh Token: $refresh" }

                        refreshRepository.save(Refresh(null, username, refresh, 60 * 60 * 1000))

                        val response = exchange.response
                        response.statusCode = HttpStatus.OK
                        response.headers.add("access", access)
                        response.addCookie(createCookie("refresh", refresh))

                        response.setComplete()
                    }
            }
    }
}

data class LoginRequest(
    val username: String,
    val password: String
)