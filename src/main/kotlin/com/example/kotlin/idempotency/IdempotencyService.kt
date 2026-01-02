package com.example.kotlin.idempotency

import com.example.kotlin.config.Loggable
import com.example.kotlin.reserve.dto.ReserveResponse
import com.example.kotlin.reserveException.ReserveException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class IdempotencyService(
    private val idempotencyRepository: IdempotencyRepository,
    private val objectMapper: ObjectMapper
) : Loggable {

    fun execute(
        idempotencyKey: String,
        httpMethod: String,
        task: () -> ReserveResponse
    ): ResponseEntity<Any> {

        // 기존 이력 확인
        val savedIdempotency = idempotencyRepository.findByIdempotencyKey(idempotencyKey)

        if (savedIdempotency != null) {
            if (savedIdempotency.expiresAt.isAfter(LocalDateTime.now())) {
                log.info { "동일한 요청 감지 - 키: $idempotencyKey" }

                return ResponseEntity
                    .status(savedIdempotency.statusCode)
                    .body(objectMapper.readTree(savedIdempotency.responseBody)) // JSON 객체로 파싱하여 반환
            }
        }

        return try {
            // 로직 실행
            val response = task()
            val successJson = objectMapper.writeValueAsString(response)

            // 성공 결과 저장
            saveIdempotency(idempotencyKey, httpMethod, successJson, 200)
            log.info { "멱등성 키 저장 (성공) - 키: $idempotencyKey" }

            ResponseEntity.ok(response)

        } catch (e: Exception) {
            handleException(idempotencyKey, httpMethod, e)
        }
    }

    private fun handleException(
        key: String,
        method: String,
        e: Exception
    ): ResponseEntity<Any> {
        val (status, errorCode) = when (e) {
            is ReserveException -> e.status.value() to e.errorCode.name
            else -> 500 to "INTERNAL_SERVER_ERROR"
        }

        saveIdempotency(key, method, errorCode, status)
        log.error(e) { "멱등성 키 저장 (실패) - 키: $key, 에러: $errorCode" }

        return ResponseEntity.status(status).body(errorCode)
    }

    private fun saveIdempotency(key: String, method: String, body: String, status: Int) {
        idempotencyRepository.save(
            Idempotency(
                idempotencyKey = key,
                httpMethod = method,
                responseBody = body,
                statusCode = status,
                expiresAt = LocalDateTime.now().plusMinutes(10)
            )
        )
    }
}