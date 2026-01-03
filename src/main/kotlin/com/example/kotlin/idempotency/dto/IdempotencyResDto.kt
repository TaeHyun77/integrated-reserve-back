package com.example.kotlin.idempotency.dto

data class IdempotencyResDto (
    val statusCode: Int,

    val responseBody: String
)