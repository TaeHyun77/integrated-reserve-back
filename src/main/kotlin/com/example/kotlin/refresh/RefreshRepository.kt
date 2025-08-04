package com.example.kotlin.refresh

import org.springframework.data.jpa.repository.JpaRepository
import reactor.core.publisher.Mono

interface RefreshRepository: JpaRepository<Refresh, Long> {

    fun existsByRefresh(refresh: String): Boolean

    fun deleteByRefresh(refresh: String): Boolean
}