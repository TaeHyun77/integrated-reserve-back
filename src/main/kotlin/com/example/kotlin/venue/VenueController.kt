package com.example.kotlin.venue

import com.example.kotlin.venue.dto.VenueReqDto
import com.example.kotlin.venue.dto.VenueResDto
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/venue")
@RestController
class VenueController(
    private val venueService: VenueService
) {

    @PostMapping("/register")
    fun registerVenue(@RequestBody venueReqDto: VenueReqDto): Venue {
        return venueService.registerVenue(venueReqDto)
    }

    @GetMapping("/list")
    fun venueList(): List<VenueResDto> {
        return venueService.venueList()
    }

    @DeleteMapping("/delete/{venueId}")
    fun deleteVenue(@PathVariable("venueId") venueId: Long) {
        venueService.deleteVenue(venueId)
    }
}