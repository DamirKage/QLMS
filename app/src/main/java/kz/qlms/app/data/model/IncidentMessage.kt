package kz.qlms.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * A single chat message on the `incidents/{id}/messages` subcollection —
 * a lighter, far more reliable stand-in for a live voice call: the reporter
 * and the assigned dispatcher can confirm status back and forth in real time
 * without needing a telephony/WebRTC SDK this build can't compile-test.
 */
data class IncidentMessage(
    @get:Exclude @set:Exclude var id: String = "",
    var senderId: String = "",
    var senderRole: String = Sender.CITIZEN.name,
    var text: String = "",
    @ServerTimestamp var sentAt: Date? = null,
) {
    enum class Sender { CITIZEN, DISPATCHER }

    @get:Exclude
    val sender: Sender
        get() = Sender.entries.find { it.name == senderRole } ?: Sender.CITIZEN
}
