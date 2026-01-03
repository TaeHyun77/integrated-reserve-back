package com.example.kotlin.venue.dto

import com.example.kotlin.venue.Venue

data class VenueRequest(
    val name: String,

    val location: String
) {
    fun toEntity(): Venue {
        return Venue(
            name = this.name,
            location = this.location
        )
    }
}