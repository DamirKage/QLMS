package kz.qlms.app.data.model

/**
 * Health information the user opts into sharing. It only ever leaves the device
 * as a snapshot attached to an SOS/incident the user themself triggered — it is
 * never queried or broadcast on its own, unlike a continuously-synced profile.
 */
data class MedicalProfile(
    var bloodType: String = "",
    var allergies: List<String> = emptyList(),
    var chronicConditions: List<String> = emptyList(),
    var medications: List<String> = emptyList(),
    var isOrganDonor: Boolean = false,
    var additionalNotes: String = "",
) {
    val isEmpty: Boolean
        get() = bloodType.isBlank() && allergies.isEmpty() && chronicConditions.isEmpty() &&
            medications.isEmpty() && additionalNotes.isBlank()
}
