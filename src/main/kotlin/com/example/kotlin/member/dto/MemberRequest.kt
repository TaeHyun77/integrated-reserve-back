package com.example.kotlin.member.dto

import com.example.kotlin.member.CheckUsername
import com.example.kotlin.member.Member
import com.example.kotlin.member.Role

data class MemberRequest (
    val username: String,

    val password: String,

    val name: String,

    val role: Role = Role.MEMBER,

    val email: String,

    var reward: Long
) {
    fun toEntity(password: String): Member {
        return Member(
            username = this.username,
            password = password,
            name = this.name,
            role = this.role,
            email = this.email,
            reward = 0
        )
    }
}