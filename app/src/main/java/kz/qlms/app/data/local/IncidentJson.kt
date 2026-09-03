package kz.qlms.app.data.local

import kz.qlms.app.data.model.EmergencyContact
import kz.qlms.app.data.model.Incident
import kz.qlms.app.data.model.IncidentPrivateDetails
import kz.qlms.app.data.model.MedicalProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Hand-rolled (de)serialization for a queued (incident, privateDetails) pair,
 * used only to persist a pending report/SOS in Room while offline. Kept
 * dependency-free (no Gson/Moshi) since this is the only place that needs it.
 */
data class PendingIncidentPayload(
    val incident: Incident,
    val privateDetails: IncidentPrivateDetails?,
)

fun PendingIncidentPayload.toJsonString(): String = JSONObject().apply {
    put(
        "incident",
        JSONObject().apply {
            put("id", incident.id)
            put("reporterId", incident.reporterId)
            put("reporterDisplayName", incident.reporterDisplayName)
            put("isAnonymous", incident.isAnonymous)
            put("isSosTriggered", incident.isSosTriggered)
            put("type", incident.type)
            put("description", incident.description)
            put("address", incident.address)
            put("latitude", incident.latitude)
            put("longitude", incident.longitude)
            put("geohash", incident.geohash)
            put("photoUrls", JSONArray(incident.photoUrls))
            put("status", incident.statusName)
            put("hasPrivateDetails", incident.hasPrivateDetails)
        },
    )
    privateDetails?.let { details ->
        put(
            "private",
            JSONObject().apply {
                details.medicalSnapshot?.let {
                    put(
                        "medicalSnapshot",
                        JSONObject().apply {
                            put("bloodType", it.bloodType)
                            put("allergies", JSONArray(it.allergies))
                            put("chronicConditions", JSONArray(it.chronicConditions))
                            put("medications", JSONArray(it.medications))
                            put("isOrganDonor", it.isOrganDonor)
                            put("additionalNotes", it.additionalNotes)
                        },
                    )
                }
                put(
                    "contactsSnapshot",
                    JSONArray(
                        details.contactsSnapshot.map { contact ->
                            JSONObject().apply {
                                put("id", contact.id)
                                put("name", contact.name)
                                put("relationship", contact.relationship)
                                put("phoneNumber", contact.phoneNumber)
                                put("notifyOnSos", contact.notifyOnSos)
                            }
                        },
                    ),
                )
                put("dispatcherNote", details.dispatcherNote)
            },
        )
    }
}.toString()

fun String.toPendingIncidentPayload(): PendingIncidentPayload {
    val root = JSONObject(this)
    val incidentJson = root.getJSONObject("incident")
    val incident = Incident(
        id = incidentJson.optString("id"),
        reporterId = incidentJson.optString("reporterId"),
        reporterDisplayName = incidentJson.optString("reporterDisplayName"),
        isAnonymous = incidentJson.optBoolean("isAnonymous"),
        isSosTriggered = incidentJson.optBoolean("isSosTriggered"),
        type = incidentJson.optString("type"),
        description = incidentJson.optString("description"),
        address = incidentJson.optString("address"),
        latitude = incidentJson.optDouble("latitude"),
        longitude = incidentJson.optDouble("longitude"),
        geohash = incidentJson.optString("geohash"),
        photoUrls = incidentJson.optJSONArray("photoUrls").toStringList(),
        statusName = incidentJson.optString("status"),
        hasPrivateDetails = incidentJson.optBoolean("hasPrivateDetails"),
    )

    val privateJson = root.optJSONObject("private")
    val privateDetails = privateJson?.let { p ->
        val medicalJson = p.optJSONObject("medicalSnapshot")
        val medical = medicalJson?.let {
            MedicalProfile(
                bloodType = it.optString("bloodType"),
                allergies = it.optJSONArray("allergies").toStringList(),
                chronicConditions = it.optJSONArray("chronicConditions").toStringList(),
                medications = it.optJSONArray("medications").toStringList(),
                isOrganDonor = it.optBoolean("isOrganDonor"),
                additionalNotes = it.optString("additionalNotes"),
            )
        }
        val contactsArray = p.optJSONArray("contactsSnapshot")
        val contacts = (0 until (contactsArray?.length() ?: 0)).map { i ->
            val obj = contactsArray!!.getJSONObject(i)
            EmergencyContact(
                id = obj.optString("id"),
                name = obj.optString("name"),
                relationship = obj.optString("relationship"),
                phoneNumber = obj.optString("phoneNumber"),
                notifyOnSos = obj.optBoolean("notifyOnSos", true),
            )
        }
        IncidentPrivateDetails(medicalSnapshot = medical, contactsSnapshot = contacts, dispatcherNote = p.optString("dispatcherNote"))
    }

    return PendingIncidentPayload(incident, privateDetails)
}

fun List<String>.toJsonArrayString(): String = JSONArray(this).toString()

fun String.toStringListFromJsonArray(): List<String> = runCatching { JSONArray(this).toStringList() }.getOrDefault(emptyList())

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).map { getString(it) }
}
