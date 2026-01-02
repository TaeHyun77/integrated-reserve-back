package com.example.kotlin.performance.repository

import com.example.kotlin.performance.Performance

interface PerformanceRepositoryCustom {

    fun findPerformancesByVenueId(venueId: Long): List<Performance>

}