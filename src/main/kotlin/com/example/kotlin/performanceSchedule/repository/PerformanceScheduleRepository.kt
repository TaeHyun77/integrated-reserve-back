package com.example.kotlin.performanceSchedule.repository

import com.example.kotlin.performanceSchedule.PerformanceSchedule
import org.springframework.data.jpa.repository.JpaRepository

interface PerformanceScheduleRepository: JpaRepository<PerformanceSchedule, Long>, PerformanceScheduleRepositoryCustom {
}