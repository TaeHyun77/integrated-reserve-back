package com.example.kotlin.member.dto

import java.time.LocalDate

data class MemberRewardResponse (
    val username: String,

    val reward: Long,

    val lastRewardDate: LocalDate?
)