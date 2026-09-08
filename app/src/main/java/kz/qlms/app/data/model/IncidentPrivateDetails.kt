package kz.qlms.app.data.model

/**
 * Sensitive fields split out of [Incident] into a `incidents/{id}/private/dispatch`
 * subdocument. This is a deliberate correction versus a flat single-document
 * design: Firestore security rules can only grant or deny an entire document,
 * never individual fields, so if medical info and emergency contacts lived on
 * the same doc that the public "nearby incidents" feed queries, every citizen
 * browsing that feed would be able to read every SOS caller's blood type,
 * allergies, and family phone numbers. Keeping them in a separate doc lets the
 * public doc stay readable by anyone nearby while this one stays readable only
 * by the reporter and dispatcher/admin accounts (see firestore.rules).
 */
data class IncidentPrivateDetails(
    var medicalSnapshot: MedicalProfile? = null,
    var contactsSnapshot: List<EmergencyContact> = emptyList(),
    var dispatcherNote: String = "",
    /**
     * Peak impact g-force at the moment [kz.qlms.app.util.CrashHeuristic] detected a crash —
     * eCall-style telemetry the dispatcher can act on immediately ("this was a hard hit") rather
     * than an unqualified "possible crash detected". Null for every non-crash-triggered incident.
     */
    var impactForceG: Double? = null,
) {
    val isEmpty: Boolean
        get() = (medicalSnapshot == null || medicalSnapshot?.isEmpty == true) &&
            contactsSnapshot.isEmpty() && dispatcherNote.isBlank() && impactForceG == null
}
