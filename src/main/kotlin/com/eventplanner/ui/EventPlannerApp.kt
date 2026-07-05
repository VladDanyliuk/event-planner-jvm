package com.eventplanner.ui

import com.eventplanner.domain.EventManager
import com.eventplanner.persistence.DataStore
import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

class EventPlannerApp : Application() {
    companion object {
        val eventManager = EventManager()
        val dataStore = DataStore()
    }

    override fun start(primaryStage: Stage) {
        loadData()

        primaryStage.title = "Event Planning Application"

        val mainPane = BorderPane()
        mainPane.padding = Insets(10.0)

        val menuBar = createMenuBar(primaryStage)
        mainPane.top = menuBar

        val tabPane = TabPane()

        val venueTab = Tab("Venues", VenueView(eventManager, dataStore).root)
        venueTab.isClosable = false

        val eventTab = Tab("Events", EventView(eventManager, dataStore).root)
        eventTab.isClosable = false

        val participantTab = Tab("Participants", ParticipantView(eventManager, dataStore).root)
        participantTab.isClosable = false

        val registrationTab = Tab("Registration", RegistrationView(eventManager, dataStore).root)
        registrationTab.isClosable = false

        val schedulingTab = Tab("Smart Scheduling", SchedulingView(eventManager, dataStore).root)
        schedulingTab.isClosable = false

        tabPane.tabs.addAll(venueTab, eventTab, participantTab, registrationTab, schedulingTab)
        mainPane.center = tabPane

        val scene = Scene(mainPane, 900.0, 600.0)
        primaryStage.scene = scene
        primaryStage.show()

        primaryStage.setOnCloseRequest {
            saveData()
        }
    }

    private fun createMenuBar(stage: Stage): MenuBar {
        val menuBar = MenuBar()

        val fileMenu = Menu("File")

        val saveItem = MenuItem("Save")
        saveItem.setOnAction {
            if (saveData()) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Data saved successfully!")
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save data!")
            }
        }

        val loadItem = MenuItem("Load")
        loadItem.setOnAction {
            if (loadData()) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Data loaded successfully!")
                stage.close()
                start(Stage())
            } else {
                showAlert(Alert.AlertType.WARNING, "Warning", "No saved data found!")
            }
        }

        val exitItem = MenuItem("Exit")
        exitItem.setOnAction {
            saveData()
            stage.close()
        }

        fileMenu.items.addAll(saveItem, loadItem, SeparatorMenuItem(), exitItem)
        menuBar.menus.add(fileMenu)

        return menuBar
    }

    private fun saveData(): Boolean {
        return dataStore.save(
            eventManager.getAllVenues(),
            eventManager.getAllParticipants(),
            eventManager.getAllEvents()
        )
    }

    private fun loadData(): Boolean {
        val data = dataStore.load() ?: return false
        eventManager.loadData(data.venues, data.participants, data.events)
        return true
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        val alert = Alert(type)
        alert.title = title
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }
}
