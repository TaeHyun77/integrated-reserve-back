package com.example.kotlin.outbox

import org.springframework.data.jpa.repository.JpaRepository

interface OutboxRepository: JpaRepository<Outbox, Long> {

    fun findByUserId(userId: String): Outbox?

}