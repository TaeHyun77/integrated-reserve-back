package com.example.kotlin.refresh

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class ReissueController(
    private val reissueService: ReissueService
) {

    @PostMapping("/reToken")
    fun reToken(exchange: ServerWebExchange): Mono<Void> {
        return reissueService.reToken(exchange)
    }
}