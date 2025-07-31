package com.example.kotlin.queueing.kafka

data class DebeziumKafkaMessage(
    var payload: Payload?
) {
    data class Payload(
        var id: Long? = null,
        var queue_type: String? = null, // 스네이크 표기법은 카프카에 저장될 때의 형식임, 따라서 맞춰줘야 헷갈리지 않음
        var status: String? = null,
        var user_id: String? = null
    )
}