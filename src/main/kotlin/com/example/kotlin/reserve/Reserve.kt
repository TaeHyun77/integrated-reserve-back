package com.example.kotlin.reserve

import com.example.kotlin.BaseTime
import com.example.kotlin.member.Member
import com.example.kotlin.seat.Seat
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class Reserve(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="reserve_id")
    val id: Long? = null,

    val reservationNumber: String,

    val reservedBy: String,

    val totalAmount: Long,

    val rewardDiscountAmount: Long,

    val finalAmount: Long,

    val reservedSeat: List<String>,

    val performanceScheduleId: Long?,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    val member: Member,

    @OneToMany(mappedBy = "reserve")
    val seatList: MutableList<Seat> = mutableListOf()
): BaseTime()