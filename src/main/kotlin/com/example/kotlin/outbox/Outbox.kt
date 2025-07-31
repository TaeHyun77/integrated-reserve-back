package com.example.kotlin.outbox

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Outbox (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val queueType: String,

    var status: String,

    val userId: String
) {
    fun updateUserStatus(status: String) {
        this.status = status
    }
}