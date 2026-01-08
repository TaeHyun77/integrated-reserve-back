package com.example.kotlin.reserve

import com.example.kotlin.reserve.dto.ReserveResponse
import org.springframework.data.jpa.repository.JpaRepository

interface ReserveRepository: JpaRepository<Reserve, Long> {

    fun findByReservationNumber(reservationNumber: String): Reserve?

    fun findByReservedBy(username: String): List<Reserve>
}