package com.example.kotlin.idempotency.dto

data class IdempotencyResponse (
    val statusCode: Int,

    val responseBody: String
)