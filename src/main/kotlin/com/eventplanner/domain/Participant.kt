package com.eventplanner.domain

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Participant(
    val id: String = UUID.randomUUID().toString(),
    var firstName: String,
    var lastName: String,
    var email: String,
    var phoneNumber: String = "",
    var organization: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName"

    override fun toString(): String {
        return "$fullName ($email)"
    }
}
