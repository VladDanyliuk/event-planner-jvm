package com.eventplanner.ui

import com.eventplanner.domain.EventManager
import com.eventplanner.domain.Participant
import com.eventplanner.persistence.DataStore
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class ParticipantView(private val eventManager: EventManager, private val dataStore: DataStore) {
    val root = VBox(10.0)
    private val participantListView = ListView<Participant>()
    private val participantList = FXCollections.observableArrayList<Participant>()

    init {
        root.padding = Insets(10.0)
        setupUI()
        refreshParticipantList()
    }

    private fun setupUI() {
        val title = Label("Participant Management")
        title.style = "-fx-font-size: 18px; -fx-font-weight: bold;"

        val formGrid = GridPane()
        formGrid.hgap = 10.0
        formGrid.vgap = 10.0
        formGrid.padding = Insets(10.0)

        val firstNameField = TextField()
        val lastNameField = TextField()
        val emailField = TextField()
        val phoneField = TextField()
        val organizationField = TextField()

        formGrid.add(Label("First Name:"), 0, 0)
        formGrid.add(firstNameField, 1, 0)
        formGrid.add(Label("Last Name:"), 0, 1)
        formGrid.add(lastNameField, 1, 1)
        formGrid.add(Label("Email:"), 0, 2)
        formGrid.add(emailField, 1, 2)
        formGrid.add(Label("Phone:"), 0, 3)
        formGrid.add(phoneField, 1, 3)
        formGrid.add(Label("Organization:"), 0, 4)
        formGrid.add(organizationField, 1, 4)

        val addButton = Button("Add Participant")
        addButton.setOnAction {
            try {
                if (firstNameField.text.isBlank() || lastNameField.text.isBlank() || emailField.text.isBlank()) {
                    showAlert(Alert.AlertType.WARNING, "Warning", "First name, last name, and email are required!")
                    return@setOnAction
                }

                val participant = Participant(
                    firstName = firstNameField.text,
                    lastName = lastNameField.text,
                    email = emailField.text,
                    phoneNumber = phoneField.text,
                    organization = organizationField.text
                )
                eventManager.addParticipant(participant)
                refreshParticipantList()
                clearForm(firstNameField, lastNameField, emailField, phoneField, organizationField)
                showAlert(Alert.AlertType.INFORMATION, "Success", "Participant added successfully!")
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add participant: ${e.message}")
            }
        }

        formGrid.add(addButton, 1, 5)

        participantListView.items = participantList
        participantListView.prefHeight = 300.0

        val deleteButton = Button("Delete Selected Participant")
        deleteButton.setOnAction {
            val selectedParticipant = participantListView.selectionModel.selectedItem
            if (selectedParticipant != null) {
                val confirmation = Alert(Alert.AlertType.CONFIRMATION)
                confirmation.title = "Confirm Deletion"
                confirmation.headerText = "Delete participant: ${selectedParticipant.fullName}?"
                confirmation.contentText = "This action cannot be undone."

                val result = confirmation.showAndWait()
                if (result.isPresent && result.get() == ButtonType.OK) {
                    eventManager.removeParticipant(selectedParticipant.id)
                    refreshParticipantList()
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Participant deleted!")
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a participant to delete!")
            }
        }

        val buttonBox = HBox(10.0, deleteButton)
        buttonBox.padding = Insets(10.0, 0.0, 0.0, 0.0)

        root.children.addAll(
            title,
            formGrid,
            Separator(),
            Label("Registered Participants:"),
            participantListView,
            buttonBox
        )
    }

    private fun refreshParticipantList() {
        participantList.clear()
        participantList.addAll(eventManager.getAllParticipants())
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
