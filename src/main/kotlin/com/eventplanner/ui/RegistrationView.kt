package com.eventplanner.ui

import com.eventplanner.domain.Event
import com.eventplanner.domain.EventManager
import com.eventplanner.domain.Participant
import com.eventplanner.persistence.DataStore
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class RegistrationView(private val eventManager: EventManager, private val dataStore: DataStore) {
    val root = VBox(10.0)
    private val eventCombo = ComboBox<Event>()
    private val participantCombo = ComboBox<Participant>()
    private val registeredListView = ListView<Participant>()
    private val registeredList = FXCollections.observableArrayList<Participant>()

    init {
        root.padding = Insets(10.0)
        setupUI()
        refreshCombos()
    }

    private fun setupUI() {
        val title = Label("Participant Registration")
        title.style = "-fx-font-size: 18px; -fx-font-weight: bold;"

        val formGrid = GridPane()
        formGrid.hgap = 10.0
        formGrid.vgap = 10.0
        formGrid.padding = Insets(10.0)

        eventCombo.prefWidth = 400.0
        participantCombo.prefWidth = 400.0

        formGrid.add(Label("Select Event:"), 0, 0)
        formGrid.add(eventCombo, 1, 0)
        formGrid.add(Label("Select Participant:"), 0, 1)
        formGrid.add(participantCombo, 1, 1)

        val eventInfoLabel = Label()
        eventInfoLabel.style = "-fx-text-fill: #666;"
        formGrid.add(eventInfoLabel, 1, 2)

        eventCombo.setOnAction {
            val selectedEvent = eventCombo.value
            if (selectedEvent != null) {
                val venue = eventManager.getVenue(selectedEvent.venueId)
                eventInfoLabel.text = "Capacity: ${selectedEvent.participantIds.size}/${selectedEvent.maxParticipants} " +
                        "| Venue: ${venue?.name ?: "Unknown"} | Date: ${selectedEvent.date}"
                refreshRegisteredList()
            } else {
                eventInfoLabel.text = ""
                registeredList.clear()
            }
        }

        val registerButton = Button("Register Participant")
        registerButton.setOnAction {
            val selectedEvent = eventCombo.value
            val selectedParticipant = participantCombo.value

            if (selectedEvent == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an event!")
                return@setOnAction
            }

            if (selectedParticipant == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a participant!")
                return@setOnAction
            }

            if (!selectedEvent.canAddParticipant()) {
                showAlert(
                    Alert.AlertType.ERROR,
                    "Error",
                    "Event is at full capacity (${selectedEvent.maxParticipants} participants)!"
                )
                return@setOnAction
            }

            if (eventManager.registerParticipantToEvent(selectedEvent.id, selectedParticipant.id)) {
                showAlert(
                    Alert.AlertType.INFORMATION,
                    "Success",
                    "${selectedParticipant.fullName} registered to ${selectedEvent.title}!"
                )
                refreshRegisteredList()
                refreshCombos()
                eventCombo.value = selectedEvent // Restore selection
                val venue = eventManager.getVenue(selectedEvent.venueId)
                eventInfoLabel.text = "Capacity: ${selectedEvent.participantIds.size}/${selectedEvent.maxParticipants} " +
                        "| Venue: ${venue?.name ?: "Unknown"} | Date: ${selectedEvent.date}"
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to register participant (already registered?)!")
            }
        }

        val refreshButton = Button("Refresh Lists")
        refreshButton.setOnAction {
            refreshCombos()
            refreshRegisteredList()
        }

        val buttonBox = HBox(10.0, registerButton, refreshButton)
        formGrid.add(buttonBox, 1, 3)

        registeredListView.items = registeredList
        registeredListView.prefHeight = 250.0

        val unregisterButton = Button("Unregister Selected")
        unregisterButton.setOnAction {
            val selectedEvent = eventCombo.value
            val selectedParticipant = registeredListView.selectionModel.selectedItem

            if (selectedEvent == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select an event!")
                return@setOnAction
            }

            if (selectedParticipant == null) {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a participant to unregister!")
                return@setOnAction
            }

            val confirmation = Alert(Alert.AlertType.CONFIRMATION)
            confirmation.title = "Confirm Unregistration"
            confirmation.headerText = "Unregister ${selectedParticipant.fullName} from ${selectedEvent.title}?"
            confirmation.contentText = "This action cannot be undone."

            val result = confirmation.showAndWait()
            if (result.isPresent && result.get() == ButtonType.OK) {
                if (eventManager.unregisterParticipantFromEvent(selectedEvent.id, selectedParticipant.id)) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Participant unregistered!")
                    refreshRegisteredList()
                    refreshCombos()
                    eventCombo.value = selectedEvent // Restore selection
                    val venue = eventManager.getVenue(selectedEvent.venueId)
                    eventInfoLabel.text = "Capacity: ${selectedEvent.participantIds.size}/${selectedEvent.maxParticipants} " +
                            "| Venue: ${venue?.name ?: "Unknown"} | Date: ${selectedEvent.date}"
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to unregister participant!")
                }
            }
        }

        val unregisterButtonBox = HBox(10.0, unregisterButton)
        unregisterButtonBox.padding = Insets(10.0, 0.0, 0.0, 0.0)

        root.children.addAll(
            title,
            formGrid,
            Separator(),
            Label("Registered Participants for Selected Event:"),
            registeredListView,
            unregisterButtonBox
        )
    }

    private fun refreshCombos() {
        val currentEvent = eventCombo.value
        eventCombo.items = FXCollections.observableArrayList(eventManager.getAllEvents())
        if (currentEvent != null) {
            eventCombo.value = eventManager.getEvent(currentEvent.id)
        }

        participantCombo.items = FXCollections.observableArrayList(eventManager.getAllParticipants())
    }

    private fun refreshRegisteredList() {
        val selectedEvent = eventCombo.value
        registeredList.clear()
        if (selectedEvent != null) {
            registeredList.addAll(eventManager.getEventParticipants(selectedEvent.id))
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
