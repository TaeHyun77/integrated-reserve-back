package com.example.kotlin.seat

import com.example.kotlin.reserve.Reserve
import com.example.kotlin.performanceSchedule.PerformanceSchedule
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
class Seat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performanceSchedule_id")
    val performanceSchedule: PerformanceSchedule,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserve_id")
    var reserve: Reserve? = null,

    // 좌석은 A1 ~ A5 , B1 ~ B5 까지만 있다고 가정
    val seatNumber: String?,

    var isReserved: Boolean?
)