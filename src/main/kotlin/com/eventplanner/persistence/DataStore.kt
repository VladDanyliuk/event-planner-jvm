package com.eventplanner.persistence

import com.eventplanner.domain.Event
import com.eventplanner.domain.Participant
import com.eventplanner.domain.Venue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class EventPlannerData(
    val venues: List<Venue>,
    val participants: List<Participant>,
    val events: List<Event>
)

class DataStore(private val filePath: String = "event-planner-data.json") {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun save(venues: List<Venue>, participants: List<Participant>, events: List<Event>): Boolean {
        return try {
            val data = EventPlannerData(venues, participants, events)
            val jsonString = json.encodeToString(data)
            File(filePath).writeText(jsonString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun load(): EventPlannerData? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return null
            }
            val jsonString = file.readText()
            json.decodeFromString<EventPlannerData>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun fileExists(): Boolean {
        return File(filePath).exists()
    }

    fun deleteFile(): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }
}
