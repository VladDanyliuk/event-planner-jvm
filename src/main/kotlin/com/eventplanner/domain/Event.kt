package com.eventplanner.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Serializable
data class Event(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    @Serializable(with = LocalDateSerializer::class)
    var date: LocalDate,
    @Serializable(with = LocalTimeSerializer::class)
    var startTime: LocalTime,
    @Serializable(with = LocalTimeSerializer::class)
    var endTime: LocalTime,
    var venueId: String,
    var description: String = "",
    var maxParticipants: Int = 0,
    var participantIds: MutableList<String> = mutableListOf()
) {
    fun canAddParticipant(): Boolean {
        return participantIds.size < maxParticipants
    }

    fun addParticipant(participantId: String): Boolean {
        if (canAddParticipant() && !participantIds.contains(participantId)) {
            participantIds.add(participantId)
            return true
        }
        return false
    }

    fun removeParticipant(participantId: String): Boolean {
        return participantIds.remove(participantId)
    }

    fun getRemainingCapacity(): Int {
        return maxParticipants - participantIds.size
    }

    override fun toString(): String {
        return "$title on $date at $startTime (${participantIds.size}/$maxParticipants participants)"
    }
}
