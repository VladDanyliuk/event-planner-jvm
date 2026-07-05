package com.eventplanner.domain

import java.time.LocalDate
import java.time.LocalTime

class EventManager {
    private val venues = mutableListOf<Venue>()
    private val participants = mutableListOf<Participant>()
    private val events = mutableListOf<Event>()

    fun addVenue(venue: Venue) {
        venues.add(venue)
    }

    fun removeVenue(venueId: String): Boolean {
        return venues.removeIf { it.id == venueId }
    }

    fun getVenue(venueId: String): Venue? {
        return venues.find { it.id == venueId }
    }

    fun getAllVenues(): List<Venue> {
        return venues.toList()
    }

    fun addParticipant(participant: Participant) {
        participants.add(participant)
    }

    fun removeParticipant(participantId: String): Boolean {
        return participants.removeIf { it.id == participantId }
    }

    fun getParticipant(participantId: String): Participant? {
        return participants.find { it.id == participantId }
    }

    fun getAllParticipants(): List<Participant> {
        return participants.toList()
    }

    fun addEvent(event: Event): Boolean {
        val venue = getVenue(event.venueId) ?: return false
        if (!venue.isAvailableForEvent(event.maxParticipants)) {
            return false
        }
        events.add(event)
        return true
    }

    fun removeEvent(eventId: String): Boolean {
        return events.removeIf { it.id == eventId }
    }

    fun getEvent(eventId: String): Event? {
        return events.find { it.id == eventId }
    }

    fun getAllEvents(): List<Event> {
        return events.toList()
    }

    fun registerParticipantToEvent(eventId: String, participantId: String): Boolean {
        val event = getEvent(eventId) ?: return false
        val participant = getParticipant(participantId) ?: return false
        return event.addParticipant(participantId)
    }

    fun unregisterParticipantFromEvent(eventId: String, participantId: String): Boolean {
        val event = getEvent(eventId) ?: return false
        return event.removeParticipant(participantId)
    }

    fun getEventParticipants(eventId: String): List<Participant> {
        val event = getEvent(eventId) ?: return emptyList()
        return event.participantIds.mapNotNull { getParticipant(it) }
    }

    fun findAvailableVenues(capacity: Int, date: LocalDate, startTime: LocalTime, endTime: LocalTime): List<Venue> {
        return venues.filter { venue ->
            venue.capacity >= capacity &&
                    !isVenueBooked(venue.id, date, startTime, endTime)
        }
    }

    private fun isVenueBooked(venueId: String, date: LocalDate, startTime: LocalTime, endTime: LocalTime): Boolean {
        return events.any { event ->
            event.venueId == venueId &&
                    event.date == date &&
                    timesOverlap(event.startTime, event.endTime, startTime, endTime)
        }
    }

    private fun timesOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean {
        return start1 < end2 && start2 < end1
    }

    fun hasConflict(venueId: String, date: LocalDate, startTime: LocalTime, endTime: LocalTime, excludeEventId: String? = null): Boolean {
        return events.any { event ->
            event.id != excludeEventId &&
                    event.venueId == venueId &&
                    event.date == date &&
                    timesOverlap(event.startTime, event.endTime, startTime, endTime)
        }
    }

    fun clear() {
        venues.clear()
        participants.clear()
        events.clear()
    }

    fun loadData(newVenues: List<Venue>, newParticipants: List<Participant>, newEvents: List<Event>) {
        clear()
        venues.addAll(newVenues)
        participants.addAll(newParticipants)
        events.addAll(newEvents)
    }
}
