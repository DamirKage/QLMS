package kz.qlms.app.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Public-ish account profile. Deliberately does NOT store the user's IIN
 * (Individual Identification Number) — the original thesis design collected
 * it as a required field, but an IIN is a highly sensitive national identifier
 * in Kazakhstan and this app has no real need for it: Firebase Auth (phone/email)
 * is already enough to identify a reporter to a dispatcher. Collecting it would
 * have been unnecessary exposure under the "On Personal Data" law.
 */
data class UserProfile(
    @get:Exclude @set:Exclude var uid: String = "",
    var fullName: String = "",
    var phoneNumber: String = "",
    var email: String = "",
    var photoUrl: String = "",
    var preferredLanguage: String = "kk",
    var role: String = Role.CITIZEN.name,
    @ServerTimestamp var createdAt: Date? = null,
) {
    enum class Role { CITIZEN, DISPATCHER, ADMIN }

    @get:Exclude
    val userRole: Role
        get() = Role.entries.find { it.name == role } ?: Role.CITIZEN
}
