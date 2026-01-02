package com.example.kotlin.performance.dto

import com.example.kotlin.performance.ScreenInfoListResponse


data class PerformanceResponse(
    val id: Long? = null,

    val type: String,

    val title: String,

    val duration: String,

    val price: Long,

    val screenInfoList: List<ScreenInfoListResponse>? = null
)