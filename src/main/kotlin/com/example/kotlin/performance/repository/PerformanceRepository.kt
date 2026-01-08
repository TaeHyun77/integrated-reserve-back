package com.example.kotlin.performance.repository

import com.example.kotlin.performance.Performance
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceRepository: JpaRepository<Performance, Long>, PerformanceRepositoryCustom {

}