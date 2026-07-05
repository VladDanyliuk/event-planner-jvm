package com.eventplanner.domain

import scala.jdk.javaapi.CollectionConverters
import java.time.LocalDate
import java.time.LocalTime

data class VenueSlot(
    val venue: Venue,
    val availableDate: LocalDate,
    val suggestedStartTime: LocalTime
)

data class ScheduleRequest(
    val id: String,
    val title: String,
    val requiredCapacity: Int,
    val durationHours: Int,
    val preferredDate: LocalDate,
    val preferredStartTime: LocalTime,
    val priority: Int = 0
)

data class ScheduledEvent(
    val requestId: String,
    val title: String,
    val venueId: String,
    val venueName: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
)

data class ScheduleResult(
    val scheduledEvents: List<ScheduledEvent>,
    val unscheduledRequests: List<ScheduleRequest>,
    val conflicts: List<String>
)

class SlotFinderService {

    fun findFirstAvailableSlot(
        venues: List<Venue>,
        events: List<Event>,
        requiredCapacity: Int,
        earliestDate: LocalDate,
        preferredStartTime: LocalTime,
        durationHours: Int
    ): VenueSlot? {
        val scalaVenues = convertVenuesToScala(venues)
        val scalaEvents = convertEventsToScala(events)

        val slotFinderClass = Class.forName("com.eventplanner.slotfinder.SlotFinder\$")
        val slotFinderModule = slotFinderClass.getField("MODULE\$").get(null)
        val findMethod = slotFinderClass.getMethod(
            "findFirstAvailableSlot",
            Class.forName("scala.collection.immutable.List"),
            Class.forName("scala.collection.immutable.List"),
            Int::class.javaPrimitiveType,
            LocalDate::class.java,
            LocalTime::class.java,
            Int::class.javaPrimitiveType
        )

        val result = findMethod.invoke(
            slotFinderModule,
            scalaVenues,
            scalaEvents,
            requiredCapacity,
            earliestDate,
            preferredStartTime,
            durationHours
        )

        return convertScalaOptionToVenueSlot(result, venues)
    }

    fun findAvailableVenuesForDateTime(
        venues: List<Venue>,
        events: List<Event>,
        requiredCapacity: Int,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): List<Venue> {
        val scalaVenues = convertVenuesToScala(venues)
        val scalaEvents = convertEventsToScala(events)

        val slotFinderClass = Class.forName("com.eventplanner.slotfinder.SlotFinder\$")
        val slotFinderModule = slotFinderClass.getField("MODULE\$").get(null)
        val findMethod = slotFinderClass.getMethod(
            "findAvailableVenuesForDateTime",
            Class.forName("scala.collection.immutable.List"),
            Class.forName("scala.collection.immutable.List"),
            Int::class.javaPrimitiveType,
            LocalDate::class.java,
            LocalTime::class.java,
            LocalTime::class.java
        )

        val result = findMethod.invoke(
            slotFinderModule,
            scalaVenues,
            scalaEvents,
            requiredCapacity,
            date,
            startTime,
            endTime
        )

        return convertScalaVenueListToKotlin(result, venues)
    }

    private fun convertVenuesToScala(venues: List<Venue>): Any {
        val venueInfoClass = Class.forName("com.eventplanner.slotfinder.VenueInfo")
        val constructor = venueInfoClass.getConstructor(
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java
        )

        val venueInfoList = venues.map { venue ->
            constructor.newInstance(venue.id, venue.name, venue.capacity, venue.location)
        }

        return CollectionConverters.asScala(venueInfoList).toList()
    }

    private fun convertEventsToScala(events: List<Event>): Any {
        val eventInfoClass = Class.forName("com.eventplanner.slotfinder.EventInfo")
        val constructor = eventInfoClass.getConstructor(
            String::class.java,
            String::class.java,
            LocalDate::class.java,
            LocalTime::class.java,
            LocalTime::class.java
        )

        val eventInfoList = events.map { event ->
            constructor.newInstance(event.id, event.venueId, event.date, event.startTime, event.endTime)
        }

        return CollectionConverters.asScala(eventInfoList).toList()
    }

    private fun convertScalaOptionToVenueSlot(scalaOption: Any, venues: List<Venue>): VenueSlot? {
        val optionClass = scalaOption.javaClass
        val isDefinedMethod = optionClass.getMethod("isDefined")
        val isDefined = isDefinedMethod.invoke(scalaOption) as Boolean

        if (!isDefined) return null

        val getMethod = optionClass.getMethod("get")
        val venueSlot = getMethod.invoke(scalaOption)

        val venueSlotClass = venueSlot.javaClass
        val venueMethod = venueSlotClass.getMethod("venue")
        val dateMethod = venueSlotClass.getMethod("availableDate")
        val timeMethod = venueSlotClass.getMethod("suggestedStartTime")

        val scalaVenue = venueMethod.invoke(venueSlot)
        val date = dateMethod.invoke(venueSlot) as LocalDate
        val time = timeMethod.invoke(venueSlot) as LocalTime

        val venueIdMethod = scalaVenue.javaClass.getMethod("id")
        val venueId = venueIdMethod.invoke(scalaVenue) as String

        val venue = venues.find { it.id == venueId } ?: return null

        return VenueSlot(venue, date, time)
    }

    private fun convertScalaVenueListToKotlin(scalaList: Any, venues: List<Venue>): List<Venue> {
        val javaList = CollectionConverters.asJava(scalaList as scala.collection.immutable.List<*>)
        return javaList.mapNotNull { scalaVenue ->
            val venueIdMethod = scalaVenue!!.javaClass.getMethod("id")
            val venueId = venueIdMethod.invoke(scalaVenue) as String
            venues.find { it.id == venueId }
        }
    }
}

class SchedulerService {

    fun createConflictFreeSchedule(
        requests: List<ScheduleRequest>,
        venues: List<Venue>,
        maxDaysToSearch: Int = 30
    ): ScheduleResult {
        val scalaRequests = convertScheduleRequestsToScala(requests)
        val scalaVenues = convertVenuesToScalaScheduler(venues)

        val schedulerClass = Class.forName("com.eventplanner.scheduler.EventScheduler\$")
        val schedulerModule = schedulerClass.getField("MODULE\$").get(null)
        val scheduleMethod = schedulerClass.getMethod(
            "createConflictFreeSchedule",
            Class.forName("scala.collection.immutable.List"),
            Class.forName("scala.collection.immutable.List"),
            Int::class.javaPrimitiveType
        )

        val result = scheduleMethod.invoke(schedulerModule, scalaRequests, scalaVenues, maxDaysToSearch)

        return convertScalaScheduleResultToKotlin(result)
    }

    fun createOptimizedSchedule(
        requests: List<ScheduleRequest>,
        venues: List<Venue>,
        maxDaysToSearch: Int = 30
    ): ScheduleResult {
        val scalaRequests = convertScheduleRequestsToScala(requests)
        val scalaVenues = convertVenuesToScalaScheduler(venues)

        val schedulerClass = Class.forName("com.eventplanner.scheduler.EventScheduler\$")
        val schedulerModule = schedulerClass.getField("MODULE\$").get(null)
        val scheduleMethod = schedulerClass.getMethod(
            "createOptimizedSchedule",
            Class.forName("scala.collection.immutable.List"),
            Class.forName("scala.collection.immutable.List"),
            Int::class.javaPrimitiveType
        )

        val result = scheduleMethod.invoke(schedulerModule, scalaRequests, scalaVenues, maxDaysToSearch)

        return convertScalaScheduleResultToKotlin(result)
    }

    private fun convertScheduleRequestsToScala(requests: List<ScheduleRequest>): Any {
        val requestClass = Class.forName("com.eventplanner.scheduler.ScheduleRequest")
        val constructor = requestClass.getConstructor(
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            LocalDate::class.java,
            LocalTime::class.java,
            Int::class.javaPrimitiveType
        )

        val scalaRequestList = requests.map { req ->
            constructor.newInstance(
                req.id,
                req.title,
                req.requiredCapacity,
                req.durationHours,
                req.preferredDate,
                req.preferredStartTime,
                req.priority
            )
        }

        return CollectionConverters.asScala(scalaRequestList).toList()
    }

    private fun convertVenuesToScalaScheduler(venues: List<Venue>): Any {
        val venueDataClass = Class.forName("com.eventplanner.scheduler.VenueData")
        val constructor = venueDataClass.getConstructor(
            String::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java
        )

        val venueDataList = venues.map { venue ->
            constructor.newInstance(venue.id, venue.name, venue.capacity, venue.location)
        }

        return CollectionConverters.asScala(venueDataList).toList()
    }

    private fun convertScalaScheduleResultToKotlin(scalaResult: Any): ScheduleResult {
        val resultClass = scalaResult.javaClass

        val scheduledMethod = resultClass.getMethod("scheduledEvents")
        val unscheduledMethod = resultClass.getMethod("unscheduledRequests")
        val conflictsMethod = resultClass.getMethod("conflicts")

        val scalaScheduled = scheduledMethod.invoke(scalaResult)
        val scalaUnscheduled = unscheduledMethod.invoke(scalaResult)
        val scalaConflicts = conflictsMethod.invoke(scalaResult)

        val scheduledEvents = convertScalaScheduledEvents(scalaScheduled)
        val unscheduledRequests = convertScalaUnscheduledRequests(scalaUnscheduled)
        val conflicts = CollectionConverters.asJava(scalaConflicts as scala.collection.immutable.List<*>)
            .map { it as String }

        return ScheduleResult(scheduledEvents, unscheduledRequests, conflicts)
    }

    private fun convertScalaScheduledEvents(scalaList: Any): List<ScheduledEvent> {
        val javaList = CollectionConverters.asJava(scalaList as scala.collection.immutable.List<*>)
        return javaList.map { scalaEvent ->
            val eventClass = scalaEvent!!.javaClass
            ScheduledEvent(
                requestId = eventClass.getMethod("requestId").invoke(scalaEvent) as String,
                title = eventClass.getMethod("title").invoke(scalaEvent) as String,
                venueId = eventClass.getMethod("venueId").invoke(scalaEvent) as String,
                venueName = eventClass.getMethod("venueName").invoke(scalaEvent) as String,
                date = eventClass.getMethod("date").invoke(scalaEvent) as LocalDate,
                startTime = eventClass.getMethod("startTime").invoke(scalaEvent) as LocalTime,
                endTime = eventClass.getMethod("endTime").invoke(scalaEvent) as LocalTime
            )
        }
    }

    private fun convertScalaUnscheduledRequests(scalaList: Any): List<ScheduleRequest> {
        val javaList = CollectionConverters.asJava(scalaList as scala.collection.immutable.List<*>)
        return javaList.map { scalaRequest ->
            val requestClass = scalaRequest!!.javaClass
            ScheduleRequest(
                id = requestClass.getMethod("id").invoke(scalaRequest) as String,
                title = requestClass.getMethod("title").invoke(scalaRequest) as String,
                requiredCapacity = requestClass.getMethod("requiredCapacity").invoke(scalaRequest) as Int,
                durationHours = requestClass.getMethod("durationHours").invoke(scalaRequest) as Int,
                preferredDate = requestClass.getMethod("preferredDate").invoke(scalaRequest) as LocalDate,
                preferredStartTime = requestClass.getMethod("preferredStartTime").invoke(scalaRequest) as LocalTime,
                priority = requestClass.getMethod("priority").invoke(scalaRequest) as Int
            )
        }
    }
}
