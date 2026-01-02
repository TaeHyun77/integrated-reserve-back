package com.example.kotlin.member.dto

import com.example.kotlin.member.Member
import com.example.kotlin.member.Role
import com.example.kotlin.reserve.dto.ReserveResponse
import java.time.LocalDate

data class MemberResponse(

    val id: Long? = null,

    val username: String,

    val name: String,

    val role: Role,

    val email: String,

    var last_reward_date: LocalDate?,

    val credit: Long,

    val reward: Long,

    val reserveList: List<ReserveResponse>? = null
) {
    companion object {
        fun from(member: Member): MemberResponse {
            return MemberResponse(
                id = member.id,
                username = member.username,
                name = member.name,
                role = member.role,
                email = member.email,
                last_reward_date = member.last_reward_date,
                reward = member.reward,
                credit = member.credit,
                reserveList = member.reserveList?.map {
                    ReserveResponse(
                        reservationNumber = it.reservationNumber,
                        reservedBy = username,
                        totalAmount = it.totalAmount,
                        rewardDiscountAmount = it.rewardDiscountAmount,
                        finalAmount = it.finalAmount,
                        createdAt = it.createdAt,
                        reservedSeat = it.reservedSeat,
                    )
                }
            )
        }
    }
}