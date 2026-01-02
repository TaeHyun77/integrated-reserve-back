package com.example.kotlin.seat.repository

import com.example.kotlin.seat.Seat
import org.springframework.data.jpa.repository.JpaRepository

interface SeatRepository: JpaRepository<Seat, Long>, SeatRepositoryCustom {
}