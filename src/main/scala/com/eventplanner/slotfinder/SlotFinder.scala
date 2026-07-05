package com.eventplanner.slotfinder

import java.time.{LocalDate, LocalTime}

case class VenueInfo(id: String, name: String, capacity: Int, location: String)

case class EventInfo(
    id: String,
    venueId: String,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime
)

case class VenueSlot(
    venue: VenueInfo,
    availableDate: LocalDate,
    suggestedStartTime: LocalTime
)

object SlotFinder {

  def findFirstAvailableSlot(
      venues: List[VenueInfo],
      events: List[EventInfo],
      requiredCapacity: Int,
      earliestDate: LocalDate,
      preferredStartTime: LocalTime,
      durationHours: Int
  ): Option[VenueSlot] = {

    val endTime = preferredStartTime.plusHours(durationHours)
    val suitableVenues = venues.filter(_.capacity >= requiredCapacity)

    def isVenueAvailable(venue: VenueInfo, date: LocalDate, start: LocalTime, end: LocalTime): Boolean = {
      !events.exists(event =>
        event.venueId == venue.id &&
          event.date == date &&
          timesOverlap(event.startTime, event.endTime, start, end)
      )
    }

    def timesOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean = {
      start1.isBefore(end2) && start2.isBefore(end1)
    }

    def dateStream(startDate: LocalDate): LazyList[LocalDate] = {
      startDate #:: dateStream(startDate.plusDays(1))
    }

    val result = (for {
      venue <- suitableVenues.view
      date <- dateStream(earliestDate).take(30).view
      if isVenueAvailable(venue, date, preferredStartTime, endTime)
    } yield VenueSlot(venue, date, preferredStartTime)).headOption

    result
  }

  def findAvailableVenuesForDateTime(
      venues: List[VenueInfo],
      events: List[EventInfo],
      requiredCapacity: Int,
      date: LocalDate,
      startTime: LocalTime,
      endTime: LocalTime
  ): List[VenueInfo] = {

    def timesOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean = {
      start1.isBefore(end2) && start2.isBefore(end1)
    }

    def isVenueAvailable(venue: VenueInfo): Boolean = {
      !events.exists(event =>
        event.venueId == venue.id &&
          event.date == date &&
          timesOverlap(event.startTime, event.endTime, startTime, endTime)
      )
    }

    venues
      .filter(_.capacity >= requiredCapacity)
      .filter(isVenueAvailable)
  }

  def findNextAvailableSlots(
      venue: VenueInfo,
      events: List[EventInfo],
      startDate: LocalDate,
      preferredStartTime: LocalTime,
      durationHours: Int,
      numberOfSlots: Int
  ): List[VenueSlot] = {

    val endTime = preferredStartTime.plusHours(durationHours)

    def timesOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean = {
      start1.isBefore(end2) && start2.isBefore(end1)
    }

    def isVenueAvailable(date: LocalDate): Boolean = {
      !events.exists(event =>
        event.venueId == venue.id &&
          event.date == date &&
          timesOverlap(event.startTime, event.endTime, preferredStartTime, endTime)
      )
    }

    def dateStream(date: LocalDate): LazyList[LocalDate] = {
      date #:: dateStream(date.plusDays(1))
    }

    dateStream(startDate)
      .filter(isVenueAvailable)
      .take(numberOfSlots)
      .map(date => VenueSlot(venue, date, preferredStartTime))
      .toList
  }
}
