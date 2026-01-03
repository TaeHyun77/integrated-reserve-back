package com.example.kotlin.performanceSchedule

import com.example.kotlin.performance.Performance
import com.example.kotlin.performance.repository.PerformanceRepository
import com.example.kotlin.performance.dto.PerformanceResponse
import com.example.kotlin.venue.Venue
import com.example.kotlin.venue.VenueRepository
import com.example.kotlin.reserveException.ErrorCode
import com.example.kotlin.reserveException.ReserveException
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleRequest
import com.example.kotlin.performanceSchedule.dto.PerformanceScheduleResponse
import com.example.kotlin.performanceSchedule.repository.PerformanceScheduleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PerformanceScheduleService(
    private val performanceScheduleRepository: PerformanceScheduleRepository,
    private val venueRepository: VenueRepository,
    private val performanceRepository: PerformanceRepository
) {

    @Transactional
    fun createPerformanceSchedule(performanceScheduleRequest: PerformanceScheduleRequest) {

        val venue: Venue = venueRepository.findById(performanceScheduleRequest.venueId)
            .orElseThrow { throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_PLACE_INFO) }

        val performance: Performance = performanceRepository.findById(performanceScheduleRequest.performanceId)
            .orElseThrow { throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_PERFORMANCE_INFO) }

        performanceScheduleRepository.save(performanceScheduleRequest.toEntity(venue, performance))
    }

    fun getPerformanceScheduleList(
        venueId: Long,
        performanceId: Long
    ): List<PerformanceScheduleResponse> {
        val performanceScheduleList = performanceScheduleRepository.findPerformanceScheduleListByVenueIdAndPerformanceId(venueId, performanceId)
            ?: throw ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_PERFORMANCE_SCHEDULE)

        return performanceScheduleList.map(PerformanceScheduleResponse::from)
    }

    fun getPerformanceSchedule(performanceScheduleId: Long): PerformanceSchedule {
        return performanceScheduleRepository.findById(performanceScheduleId)
            .orElseThrow {
                ReserveException(HttpStatus.BAD_REQUEST, ErrorCode.NOT_EXIST_PERFORMANCE_INFO)
            }
    }
}
