package com.eventplanner.scheduler

import java.time.{LocalDate, LocalTime}

case class ScheduleRequest(
    id: String,
    title: String,
    requiredCapacity: Int,
    durationHours: Int,
    preferredDate: LocalDate,
    preferredStartTime: LocalTime,
    priority: Int = 0
)

case class VenueData(id: String, name: String, capacity: Int, location: String)

case class ScheduledEvent(
    requestId: String,
    title: String,
    venueId: String,
    venueName: String,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime
)

case class ScheduleResult(
    scheduledEvents: List[ScheduledEvent],
    unscheduledRequests: List[ScheduleRequest],
    conflicts: List[String]
)

object EventScheduler {

  def createConflictFreeSchedule(
      requests: List[ScheduleRequest],
      venues: List[VenueData],
      maxDaysToSearch: Int = 30
  ): ScheduleResult = {

    val sortedRequests = requests.sortBy(r => (-r.priority, r.preferredDate.toEpochDay))

    def timesOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean = {
      start1.isBefore(end2) && start2.isBefore(end1)
    }

    def isSlotAvailable(
        scheduledSoFar: List[ScheduledEvent],
        venueId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean = {
      !scheduledSoFar.exists(event =>
        event.venueId == venueId &&
          event.date == date &&
          timesOverlap(event.startTime, event.endTime, startTime, endTime)
      )
    }

    def generateDateRange(startDate: LocalDate, days: Int): List[LocalDate] = {
      (0 until days).map(i => startDate.plusDays(i)).toList
    }

    def scheduleRequest(
        request: ScheduleRequest,
        scheduledSoFar: List[ScheduledEvent]
    ): Option[ScheduledEvent] = {

      val endTime = request.preferredStartTime.plusHours(request.durationHours)
      val suitableVenues = venues.filter(_.capacity >= request.requiredCapacity)
      val dateRange = generateDateRange(request.preferredDate, maxDaysToSearch)

      (for {
        date <- dateRange.view
        venue <- suitableVenues.view
        if isSlotAvailable(scheduledSoFar, venue.id, date, request.preferredStartTime, endTime)
      } yield ScheduledEvent(
        requestId = request.id,
        title = request.title,
        venueId = venue.id,
        venueName = venue.name,
        date = date,
        startTime = request.preferredStartTime,
        endTime = endTime
      )).headOption
    }

    val (scheduled, unscheduled) = sortedRequests.foldLeft((List.empty[ScheduledEvent], List.empty[ScheduleRequest])) {
      case ((scheduledAcc, unscheduledAcc), request) =>
        scheduleRequest(request, scheduledAcc) match {
          case Some(event) => (scheduledAcc :+ event, unscheduledAcc)
          case None => (scheduledAcc, unscheduledAcc :+ request)
        }
    }

    val conflicts = unscheduled.map(req =>
      s"Could not schedule '${req.title}' - no suitable venue found within ${maxDaysToSearch} days"
    )

    ScheduleResult(scheduled, unscheduled, conflicts)
  }

  def createOptimizedSchedule(
      requests: List[ScheduleRequest],
      venues: List[VenueData],
      maxDaysToSearch: Int = 30
  ): ScheduleResult = {

    val requestsByDate = requests
      .sortBy(r => (-r.priority, r.preferredDate.toEpochDay))
      .groupBy(_.preferredDate)

    def timesOverlap(start1: LocalTime, end1: LocalTime, start2: LocalTime, end2: LocalTime): Boolean = {
      start1.isBefore(end2) && start2.isBefore(end1)
    }

    def isSlotAvailable(
        scheduledSoFar: List[ScheduledEvent],
        venueId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean = {
      !scheduledSoFar.exists(event =>
        event.venueId == venueId &&
          event.date == date &&
          timesOverlap(event.startTime, event.endTime, startTime, endTime)
      )
    }

    def generateDateRange(startDate: LocalDate, days: Int): List[LocalDate] = {
      (0 until days).map(i => startDate.plusDays(i)).toList
    }

    def scheduleRequest(
        request: ScheduleRequest,
        scheduledSoFar: List[ScheduledEvent],
        preferredVenueId: Option[String] = None
    ): Option[ScheduledEvent] = {

      val endTime = request.preferredStartTime.plusHours(request.durationHours)
      val suitableVenues = venues.filter(_.capacity >= request.requiredCapacity)

      val orderedVenues = preferredVenueId match {
        case Some(venueId) =>
          suitableVenues.partition(_.id == venueId) match {
            case (preferred, others) => preferred ++ others
          }
        case None => suitableVenues
      }

      val dateRange = generateDateRange(request.preferredDate, maxDaysToSearch)

      (for {
        date <- dateRange.view
        venue <- orderedVenues.view
        if isSlotAvailable(scheduledSoFar, venue.id, date, request.preferredStartTime, endTime)
      } yield ScheduledEvent(
        requestId = request.id,
        title = request.title,
        venueId = venue.id,
        venueName = venue.name,
        date = date,
        startTime = request.preferredStartTime,
        endTime = endTime
      )).headOption
    }

    val allRequests = requestsByDate.values.flatten.toList.sortBy(r => (-r.priority, r.preferredDate.toEpochDay))

    val (scheduled, unscheduled) = allRequests.foldLeft((List.empty[ScheduledEvent], List.empty[ScheduleRequest])) {
      case ((scheduledAcc, unscheduledAcc), request) =>
        val lastVenue = scheduledAcc.lastOption.map(_.venueId)
        scheduleRequest(request, scheduledAcc, lastVenue) match {
          case Some(event) => (scheduledAcc :+ event, unscheduledAcc)
          case None => (scheduledAcc, unscheduledAcc :+ request)
        }
    }

    val conflicts = unscheduled.map(req =>
      s"Could not schedule '${req.title}' - no suitable venue found within ${maxDaysToSearch} days"
    )

    ScheduleResult(scheduled, unscheduled, conflicts)
  }
}
