package com.example.kotlin.seat.repository

import com.example.kotlin.seat.Seat

interface SeatRepositoryCustom {

    fun findSeatByScreenInfoId(screenInfoId: Long): List<Seat>

    fun findByScreenInfoAndSeatNumber(screenInfoId: Long?, seatNumber: String): Seat?
}