package com.example.kotlin.venue.dto

import com.example.kotlin.venue.Venue

data class VenueReqDto(
    val name: String,

    val location: String
) {
    fun toVenue(): Venue {
        return Venue(
            name = this.name,
            location = this.location
        )
    }
}