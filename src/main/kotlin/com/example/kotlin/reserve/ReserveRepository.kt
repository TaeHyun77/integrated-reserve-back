package com.example.kotlin.reserve

import org.springframework.data.jpa.repository.JpaRepository

interface ReserveRepository: JpaRepository<Reserve, Long> {

    fun findByReservationNumber(reservationNumber: String): Reserve?

}