package com.example.kotlin.member

import com.example.kotlin.BaseTime
import com.example.kotlin.reserve.Reserve
import jakarta.persistence.*
import java.time.LocalDate

@Entity
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true)
    val username: String,

    val password: String,

    val name: String,

    val role: Role,

    val email: String,

    var credit: Long = 30000,

    var reward: Long,

    var lastRewardDate: LocalDate? = null,

    @OneToMany(mappedBy = "member", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reserveList: List<Reserve>? = null

): BaseTime() {

    fun updateCreditAndReward(credit: Long, reward: Long) {
        this.credit -= credit
        this.reward -= reward
    }
}