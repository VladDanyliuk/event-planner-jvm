package com.eventplanner.ui

import com.eventplanner.domain.EventManager
import com.eventplanner.domain.ScheduleRequest
import com.eventplanner.domain.SchedulerService
import com.eventplanner.domain.SlotFinderService
import com.eventplanner.persistence.DataStore
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import java.time.LocalDate
import java.time.LocalTime

class SchedulingView(private val eventManager: EventManager, private val dataStore: DataStore) {
    val root = VBox(10.0)
    private val slotFinderService = SlotFinderService()
    private val schedulerService = SchedulerService()

    init {
        root.padding = Insets(10.0)
        setupUI()
    }

    private fun setupUI() {
        val title = Label("Smart Scheduling & Slot Finding")
        title.style = "-fx-font-size: 18px; -fx-font-weight: bold;"

        val slotFinderSection = createSlotFinderSection()
        val schedulerSection = createSchedulerSection()

        root.children.addAll(
            title,
            slotFinderSection,
            Separator(),
            schedulerSection
        )
    }

    private fun createSlotFinderSection(): VBox {
        val section = VBox(10.0)
        section.padding = Insets(10.0)

        val sectionTitle = Label("Find Available Venue Slot")
        sectionTitle.style = "-fx-font-size: 14px; -fx-font-weight: bold;"

        val formGrid = GridPane()
        formGrid.hgap = 10.0
        formGrid.vgap = 10.0

        val capacityField = TextField()
        val datePicker = DatePicker(LocalDate.now())
        val startTimeField = TextField("09:00")
        val durationField = TextField("2")

        formGrid.add(Label("Required Capacity:"), 0, 0)
        formGrid.add(capacityField, 1, 0)
        formGrid.add(Label("Earliest Date:"), 0, 1)
        formGrid.add(datePicker, 1, 1)
        formGrid.add(Label("Preferred Start Time (HH:MM):"), 0, 2)
        formGrid.add(startTimeField, 1, 2)
        formGrid.add(Label("Duration (hours):"), 0, 3)
        formGrid.add(durationField, 1, 3)

        val findButton = Button("Find Available Slot")
        findButton.setOnAction {
            try {
                val capacity = capacityField.text.toInt()
                val date = datePicker.value
                val startTime = LocalTime.parse(startTimeField.text)
                val duration = durationField.text.toInt()

                val slot = slotFinderService.findFirstAvailableSlot(
                    eventManager.getAllVenues(),
                    eventManager.getAllEvents(),
                    capacity,
                    date,
                    startTime,
                    duration
                )

                if (slot != null) {
                    val message = """
                        Found Available Slot!

                        Venue: ${slot.venue.name}
                        Location: ${slot.venue.location}
                        Capacity: ${slot.venue.capacity}
                        Date: ${slot.availableDate}
                        Start Time: ${slot.suggestedStartTime}
                    """.trimIndent()

                    showAlert(Alert.AlertType.INFORMATION, "Slot Found", message)
                } else {
                    showAlert(
                        Alert.AlertType.WARNING,
                        "No Slot Found",
                        "No available venue found matching your criteria within the next 30 days."
                    )
                }
            } catch (e: NumberFormatException) {
                showAlert(Alert.AlertType.ERROR, "Error", "Capacity and duration must be numbers!")
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to find slot: ${e.message}")
            }
        }

        formGrid.add(findButton, 1, 4)

        section.children.addAll(sectionTitle, formGrid)
        return section
    }

    private fun createSchedulerSection(): VBox {
        val section = VBox(10.0)
        section.padding = Insets(10.0)

        val sectionTitle = Label("Batch Event Scheduler")
        sectionTitle.style = "-fx-font-size: 14px; -fx-font-weight: bold;"

        val description = Label(
            "Create multiple event requests and the scheduler will find a conflict-free schedule."
        )
        description.style = "-fx-text-fill: #666;"
        description.isWrapText = true

        val requestArea = TextArea()
        requestArea.prefRowCount = 10
        requestArea.promptText = """Enter requests (one per line):
EventTitle,Capacity,DurationHours,PreferredDate(YYYY-MM-DD),StartTime(HH:MM),Priority

Example:
Workshop A,50,3,2025-12-01,09:00,1
Conference B,100,5,2025-12-01,10:00,2
Seminar C,30,2,2025-12-02,14:00,1
"""

        val scheduleButton = Button("Generate Conflict-Free Schedule")
        scheduleButton.setOnAction {
            try {
                val lines = requestArea.text.lines().filter { it.isNotBlank() }
                val requests = lines.map { line ->
                    val parts = line.split(",").map { it.trim() }
                    ScheduleRequest(
                        java.util.UUID.randomUUID().toString(),
                        parts[0],
                        parts[1].toInt(),
                        parts[2].toInt(),
                        LocalDate.parse(parts[3]),
                        LocalTime.parse(parts[4]),
                        if (parts.size > 5) parts[5].toInt() else 0
                    )
                }

                if (requests.isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Warning", "Please enter at least one event request!")
                    return@setOnAction
                }

                val result = schedulerService.createConflictFreeSchedule(
                    requests,
                    eventManager.getAllVenues(),
                    30
                )

                val message = buildString {
                    appendLine("Scheduling Complete!")
                    appendLine()
                    appendLine("Successfully Scheduled: ${result.scheduledEvents.size} events")
                    result.scheduledEvents.forEach { event ->
                        appendLine("  - ${event.title} at ${event.venueName} on ${event.date} ${event.startTime}-${event.endTime}")
                    }
                    appendLine()
                    if (result.unscheduledRequests.isNotEmpty()) {
                        appendLine("Could Not Schedule: ${result.unscheduledRequests.size} events")
                        result.unscheduledRequests.forEach { req ->
                            appendLine("  - ${req.title} (capacity: ${req.requiredCapacity})")
                        }
                        appendLine()
                        appendLine("Conflicts:")
                        result.conflicts.forEach { conflict ->
                            appendLine("  - $conflict")
                        }
                    }
                }

                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = "Scheduling Results"
                alert.headerText = null
                val textArea = TextArea(message)
                textArea.isEditable = false
                textArea.isWrapText = true
                textArea.prefRowCount = 20
                alert.dialogPane.content = textArea
                alert.showAndWait()

            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to create schedule: ${e.message}")
            }
        }

        section.children.addAll(sectionTitle, description, requestArea, scheduleButton)
        return section
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
