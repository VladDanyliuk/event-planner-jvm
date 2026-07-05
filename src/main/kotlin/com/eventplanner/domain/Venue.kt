package com.eventplanner.domain

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Venue(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var capacity: Int,
    var location: String,
    var description: String = "",
    var facilities: String = ""
) {
    override fun toString(): String {
        return "$name (Capacity: $capacity, Location: $location)"
    }

    fun isAvailableForEvent(requiredCapacity: Int): Boolean {
        return capacity >= requiredCapacity
    }
}
