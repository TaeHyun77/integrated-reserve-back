package com.example.kotlin.reserve

import com.example.kotlin.config.Loggable
import com.example.kotlin.reserve.dto.ReserveRequest
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/reserve")
@RestController
class ReserveController(
    private val reserveService: ReserveService
): Loggable {

    @PostMapping("/reserve")
    fun reserveSeat(
        @RequestBody reserveRequest: ReserveRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<String> {

        val idempotencyKey: String = httpRequest.getHeader("Idempotency-key")
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_IN_HEADER_IDEMPOTENCY_KEY)

        log.info { "idempotencyKey : $idempotencyKey" }

        return reserveService.reserveSeat(reserveRequest, idempotencyKey)
    }

    // 예약 취소
    @DeleteMapping("/delete/{reserveNumber}")
    fun deleteReserveInfo(
        @PathVariable("reserveNumber") reserveNumber: String,
        request: HttpServletRequest
    ) {

        val idempotencyKey: String = request.getHeader("Idempotency-key")
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_IN_HEADER_IDEMPOTENCY_KEY)

        log.info { "idempotencyKey: $idempotencyKey" }

        reserveService.deleteReserveInfo(reserveNumber, idempotencyKey)
    }
}