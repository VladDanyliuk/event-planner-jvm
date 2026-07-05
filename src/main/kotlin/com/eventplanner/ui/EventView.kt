package com.eventplanner.ui

import com.eventplanner.domain.Event
import com.eventplanner.domain.EventManager
import com.eventplanner.domain.Venue
import com.eventplanner.persistence.DataStore
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import java.time.LocalDate
import java.time.LocalTime

class EventView(private val eventManager: EventManager, private val dataStore: DataStore) {
    val root = VBox(10.0)
    private val eventListView = ListView<Event>()
    private val eventList = FXCollections.observableArrayList<Event>()

    init {
        root.padding = Insets(10.0)
        setupUI()
        refreshEventList()
    }

    private fun setupUI() {
        val title = Label("Event Management")
        title.style = "-fx-font-size: 18px; -fx-font-weight: bold;"

        val formGrid = GridPane()
        formGrid.hgap = 10.0
        formGrid.vgap = 10.0
        formGrid.padding = Insets(10.0)

        val titleField = TextField()
        val datePicker = DatePicker(LocalDate.now())
        val startTimeField = TextField("09:00")
        val endTimeField = TextField("17:00")
        val venueCombo = ComboBox<Venue>()
        venueCombo.items = FXCollections.observableArrayList(eventManager.getAllVenues())
        val descriptionField = TextField()
        val maxParticipantsField = TextField()

        formGrid.add(Label("Title:"), 0, 0)
        formGrid.add(titleField, 1, 0)
        formGrid.add(Label("Date:"), 0, 1)
        formGrid.add(datePicker, 1, 1)
        formGrid.add(Label("Start Time (HH:MM):"), 0, 2)
        formGrid.add(startTimeField, 1, 2)
        formGrid.add(Label("End Time (HH:MM):"), 0, 3)
        formGrid.add(endTimeField, 1, 3)
        formGrid.add(Label("Venue:"), 0, 4)
        formGrid.add(venueCombo, 1, 4)
        formGrid.add(Label("Description:"), 0, 5)
        formGrid.add(descriptionField, 1, 5)
        formGrid.add(Label("Max Participants:"), 0, 6)
        formGrid.add(maxParticipantsField, 1, 6)

        val addButton = Button("Add Event")
        addButton.setOnAction {
            try {
                val selectedVenue = venueCombo.value
                if (selectedVenue == null) {
                    showAlert(Alert.AlertType.WARNING, "Warning", "Please select a venue!")
                    return@setOnAction
                }

                val maxParticipants = maxParticipantsField.text.toInt()
                if (maxParticipants > selectedVenue.capacity) {
                    showAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "Max participants ($maxParticipants) exceeds venue capacity (${selectedVenue.capacity})!"
                    )
                    return@setOnAction
                }

                val startTime = LocalTime.parse(startTimeField.text)
                val endTime = LocalTime.parse(endTimeField.text)
                val date = datePicker.value

                if (eventManager.hasConflict(selectedVenue.id, date, startTime, endTime)) {
                    showAlert(
                        Alert.AlertType.ERROR,
                        "Error",
                        "This venue is already booked for the selected time!"
                    )
                    return@setOnAction
                }

                val event = Event(
                    title = titleField.text,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    venueId = selectedVenue.id,
                    description = descriptionField.text,
                    maxParticipants = maxParticipants
                )

                if (eventManager.addEvent(event)) {
                    refreshEventList()
                    clearForm(titleField, descriptionField, maxParticipantsField, startTimeField, endTimeField)
                    datePicker.value = LocalDate.now()
                    venueCombo.value = null
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Event added successfully!")
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to add event!")
                }
            } catch (e: NumberFormatException) {
                showAlert(Alert.AlertType.ERROR, "Error", "Max participants must be a number!")
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add event: ${e.message}")
            }
        }

        val refreshVenuesButton = Button("Refresh Venues")
        refreshVenuesButton.setOnAction {
            venueCombo.items = FXCollections.observableArrayList(eventManager.getAllVenues())
        }

        val buttonBox = HBox(10.0, addButton, refreshVenuesButton)
        formGrid.add(buttonBox, 1, 7)

        eventListView.items = eventList
        eventListView.prefHeight = 200.0

        val deleteButton = Button("Delete Selected Event")
        deleteButton.setOnAction {
            val selectedEvent = eventListView.selectionModel.selectedItem
            if (selectedEvent != null) {
                val confirmation = Alert(Alert.AlertType.CONFIRMATION)
                confirmation.title = "Confirm Deletion"
                confirmation.headerText = "Delete event: ${selectedEvent.title}?"
                confirmation.contentText = "This action cannot be undone."

                val result = confirmation.showAndWait()
                if (result.isPresent && result.get() == ButtonType.OK) {
                    eventManager.removeEvent(selectedEvent.id)
                    refreshEventList()
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Event deleted!")
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an event to delete!")
            }
        }

        val viewDetailsButton = Button("View Details")
        viewDetailsButton.setOnAction {
            val selectedEvent = eventListView.selectionModel.selectedItem
            if (selectedEvent != null) {
                val venue = eventManager.getVenue(selectedEvent.venueId)
                val participants = eventManager.getEventParticipants(selectedEvent.id)

                val details = """
                    Event: ${selectedEvent.title}
                    Date: ${selectedEvent.date}
                    Time: ${selectedEvent.startTime} - ${selectedEvent.endTime}
                    Venue: ${venue?.name ?: "Unknown"}
                    Description: ${selectedEvent.description}
                    Participants: ${participants.size}/${selectedEvent.maxParticipants}
                    Remaining Capacity: ${selectedEvent.getRemainingCapacity()}
                """.trimIndent()

                val alert = Alert(Alert.AlertType.INFORMATION)
                alert.title = "Event Details"
                alert.headerText = selectedEvent.title
                alert.contentText = details
                alert.showAndWait()
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an event to view!")
            }
        }

        val actionButtonBox = HBox(10.0, deleteButton, viewDetailsButton)
        actionButtonBox.padding = Insets(10.0, 0.0, 0.0, 0.0)

        root.children.addAll(title, formGrid, Separator(), Label("Existing Events:"), eventListView, actionButtonBox)
    }

    private fun refreshEventList() {
        eventList.clear()
        eventList.addAll(eventManager.getAllEvents())
    }

    private fun clearForm(vararg fields: TextField) {
        fields.forEach { it.clear() }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
