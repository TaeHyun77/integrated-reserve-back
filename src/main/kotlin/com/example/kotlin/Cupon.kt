package com.example.kotlin

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Cupon (

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val cuponName: String,

    var cuponCnt: Long
) {

    fun decrease() {

        if (cuponCnt < 0) {
            throw IllegalArgumentException("개수가 0 이하입니다.")
        }

        this.cuponCnt -= 1
    }
}