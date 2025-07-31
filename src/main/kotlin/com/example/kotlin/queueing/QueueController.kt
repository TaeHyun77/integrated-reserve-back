package com.example.kotlin.queueing

import com.example.kotlin.config.Loggable
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RequestMapping("/queue")
@RestController
class QueueController (
    private val queueService: QueueService
): Loggable {

    @PostMapping("/register")
    suspend fun registerUser(
        @RequestParam(defaultValue = "reserve") queueType: String, @RequestParam userId: String
    ): ResponseEntity<Any> {

        val now = Instant.now()
        val enterTimestamp = now.epochSecond * 1_000_000_000L + now.nano

        log.info { "등록 사용자 정보 ⇒ userId : $userId, enterTimestamp : $enterTimestamp" }

        val result = queueService.registerUserToWaitQueue(userId, queueType, enterTimestamp)

        return ResponseEntity.ok(result)
    }

    // 쿠키에 토큰 전달
    @GetMapping("/createCookie")
    suspend fun sendCookie(
        @RequestParam(name = "userId") userId: String, @RequestParam(defaultValue = "reserve") queueType: String, response: HttpServletResponse
    ): ResponseEntity<String> {

        return queueService.sendCookie(userId, queueType, response)
    }

}