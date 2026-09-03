package kz.qlms.app.data.remote

/** Single source of truth for collection/field names shared with firestore.rules and the web panel. */
object FirestoreSchema {
    const val USERS = "users"
    const val INCIDENTS = "incidents"

    object UserFields {
        const val MEDICAL_PROFILE = "medicalProfile" // map field on the user doc
        const val CONTACTS = "contacts" // subcollection
    }

    object IncidentFields {
        const val REPORTER_ID = "reporterId"
        const val GEOHASH = "geohash"
        const val STATUS = "status"
        const val CREATED_AT = "createdAt"
        const val IS_SOS = "isSosTriggered"
    }
}
