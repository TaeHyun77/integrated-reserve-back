package com.example.kotlin.jwt

import com.example.kotlin.member.MemberRepository
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

private val log = KotlinLogging.logger {}

@Service
class CustomUserDetailService(
    private val memberRepository: MemberRepository
) : ReactiveUserDetailsService {

    override fun findByUsername(username: String): Mono<UserDetails> {
        val member = memberRepository.findByUsername(username)
            ?: return Mono.empty()

        val userDetails = CustomUserDetails(member)
        return Mono.just(userDetails)
    }
}