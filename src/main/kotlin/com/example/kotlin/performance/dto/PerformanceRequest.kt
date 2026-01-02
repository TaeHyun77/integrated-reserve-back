package com.example.kotlin.performance.dto

import com.example.kotlin.performance.Performance

data class PerformanceRequest(
    val type: String,

    val title: String,

    val duration: String,

    val price: Long
) {
    fun toPerformance(): Performance {
        return Performance(
            type = this.type,
            title = this.title,
            duration = this.duration,
            price = this.price
        )
    }
}