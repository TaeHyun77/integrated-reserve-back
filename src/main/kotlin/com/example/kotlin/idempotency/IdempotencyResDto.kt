package com.example.kotlin.idempotency

data class IdempotencyResDto (

    val statusCode: Int,

    val responseBody: String? = null,

)