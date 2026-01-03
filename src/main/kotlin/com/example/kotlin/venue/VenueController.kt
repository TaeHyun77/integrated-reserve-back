package com.example.kotlin.venue

import com.example.kotlin.venue.dto.VenueRequest
import com.example.kotlin.venue.dto.VenueResponse
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

    @PostMapping("/create")
    fun registerVenue(@RequestBody venueRequest: VenueRequest) {
        return venueService.createVenue(venueRequest)
    }

    @GetMapping("/list")
    fun getVenueList(): List<VenueResponse> {
        return venueService.getVenueList()
    }
}