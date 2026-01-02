package com.example.kotlin.performanceSchedule

import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleRequest
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/screenInfo")
@RestController
class PerformanceScheduleController(
    private val performanceScheduleService: PerformanceScheduleService
) {

    @PostMapping("/register")
    fun registerScreen(
        @RequestBody performanceScheduleRequest: PerformanceScheduleRequest
    ) {
        performanceScheduleService.registerScreen(performanceScheduleRequest)
    }

    @GetMapping("/list/{venueId}/{performanceId}")
    fun screenInfoList(
        @PathVariable("venueId")
        venueId: Long, @PathVariable("performanceId")
        performanceId: Long
    ): List<PerformanceScheduleResponse> {
        return performanceScheduleService.screenList(venueId, performanceId)
    }
}