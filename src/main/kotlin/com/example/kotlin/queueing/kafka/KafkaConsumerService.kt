package com.example.kotlin.queueing.kafka

import com.example.kotlin.config.Loggable
import com.example.kotlin.queueing.QueueService
import com.example.kotlin.queueing.event.QueueEventPayload
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService(
    private val objectMapper: ObjectMapper,
    private val queueService: QueueService
): Loggable {

    @KafkaListener(topics = ["reserve_project.reserve_project.outbox"], groupId = "queue-event-group")
    fun consume(message: String) {
        try {
            val kafkaMessage = objectMapper.readValue(message, DebeziumKafkaMessage::class.java)
            val payload = kafkaMessage.payload

            if (payload != null) {
                val queueType = payload.queue_type
                val status = payload.status
                val userId = payload.user_id

                queueService.sink.tryEmitNext(QueueEventPayload(queueType))
                log.info{ "Kafka 이벤트 수신 - userId: $userId, queueType: $queueType, status: $status" }
            }
        } catch (e: Exception) {
            log.error{ "Kafka 메시지 consume 실패" }
        }
    }
}