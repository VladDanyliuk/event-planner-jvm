package com.eventplanner.ui

import com.eventplanner.domain.EventManager
import com.eventplanner.domain.Venue
import com.eventplanner.persistence.DataStore
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class VenueView(private val eventManager: EventManager, private val dataStore: DataStore) {
    val root = VBox(10.0)
    private val venueListView = ListView<Venue>()
    private val venueList = FXCollections.observableArrayList<Venue>()

    init {
        root.padding = Insets(10.0)
        setupUI()
        refreshVenueList()
    }

    private fun setupUI() {
        val title = Label("Venue Management")
        title.style = "-fx-font-size: 18px; -fx-font-weight: bold;"

        val formGrid = GridPane()
        formGrid.hgap = 10.0
        formGrid.vgap = 10.0
        formGrid.padding = Insets(10.0)

        val nameField = TextField()
        val capacityField = TextField()
        val locationField = TextField()
        val descriptionField = TextField()
        val facilitiesField = TextField()

        formGrid.add(Label("Name:"), 0, 0)
        formGrid.add(nameField, 1, 0)
        formGrid.add(Label("Capacity:"), 0, 1)
        formGrid.add(capacityField, 1, 1)
        formGrid.add(Label("Location:"), 0, 2)
        formGrid.add(locationField, 1, 2)
        formGrid.add(Label("Description:"), 0, 3)
        formGrid.add(descriptionField, 1, 3)
        formGrid.add(Label("Facilities:"), 0, 4)
        formGrid.add(facilitiesField, 1, 4)

        val addButton = Button("Add Venue")
        addButton.setOnAction {
            try {
                val venue = Venue(
                    name = nameField.text,
                    capacity = capacityField.text.toInt(),
                    location = locationField.text,
                    description = descriptionField.text,
                    facilities = facilitiesField.text
                )
                eventManager.addVenue(venue)
                refreshVenueList()
                clearForm(nameField, capacityField, locationField, descriptionField, facilitiesField)
                showAlert(Alert.AlertType.INFORMATION, "Success", "Venue added successfully!")
            } catch (e: NumberFormatException) {
                showAlert(Alert.AlertType.ERROR, "Error", "Capacity must be a number!")
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add venue: ${e.message}")
            }
        }

        formGrid.add(addButton, 1, 5)

        venueListView.items = venueList
        venueListView.prefHeight = 300.0

        val deleteButton = Button("Delete Selected Venue")
        deleteButton.setOnAction {
            val selectedVenue = venueListView.selectionModel.selectedItem
            if (selectedVenue != null) {
                val confirmation = Alert(Alert.AlertType.CONFIRMATION)
                confirmation.title = "Confirm Deletion"
                confirmation.headerText = "Delete venue: ${selectedVenue.name}?"
                confirmation.contentText = "This action cannot be undone."

                val result = confirmation.showAndWait()
                if (result.isPresent && result.get() == ButtonType.OK) {
                    eventManager.removeVenue(selectedVenue.id)
                    refreshVenueList()
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Venue deleted!")
                }
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "Please select a venue to delete!")
            }
        }

        val buttonBox = HBox(10.0, deleteButton)
        buttonBox.padding = Insets(10.0, 0.0, 0.0, 0.0)

        root.children.addAll(title, formGrid, Separator(), Label("Existing Venues:"), venueListView, buttonBox)
    }

    private fun refreshVenueList() {
        venueList.clear()
        venueList.addAll(eventManager.getAllVenues())
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
