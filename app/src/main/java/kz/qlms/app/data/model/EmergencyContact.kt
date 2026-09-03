package kz.qlms.app.data.model

import java.util.UUID

data class EmergencyContact(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var relationship: String = "",
    var phoneNumber: String = "",
    /** Whether this contact gets an SMS with the live location when SOS fires. */
    var notifyOnSos: Boolean = true,
)
