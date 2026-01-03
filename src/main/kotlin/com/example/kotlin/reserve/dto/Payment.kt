package com.example.kotlin.reserve.dto

data class Payment (
    val totalAmount: Long,
    
    val rewardDiscountAmount: Long,

    val finalAmount: Long
)