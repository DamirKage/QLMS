package kz.qlms.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kz.qlms.app.data.model.EmergencyContact
import kz.qlms.app.data.model.MedicalProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, encrypted cache of medical info and emergency contacts so an SOS can
 * attach them even with zero connectivity (they still sync to Firestore's own
 * offline-first cache, but this is a belt-and-suspenders copy that never
 * depends on the Firestore SDK having warmed up yet).
 */
class SecurePrefsManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "qlms_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveMedicalProfile(profile: MedicalProfile) {
        val json = JSONObject().apply {
            put("bloodType", profile.bloodType)
            put("allergies", JSONArray(profile.allergies))
            put("chronicConditions", JSONArray(profile.chronicConditions))
            put("medications", JSONArray(profile.medications))
            put("isOrganDonor", profile.isOrganDonor)
            put("additionalNotes", profile.additionalNotes)
        }
        prefs.edit().putString(KEY_MEDICAL, json.toString()).apply()
    }

    fun getMedicalProfile(): MedicalProfile {
        val raw = prefs.getString(KEY_MEDICAL, null) ?: return MedicalProfile()
        return runCatching {
            val json = JSONObject(raw)
            MedicalProfile(
                bloodType = json.optString("bloodType"),
                allergies = json.optJSONArray("allergies").toStringList(),
                chronicConditions = json.optJSONArray("chronicConditions").toStringList(),
                medications = json.optJSONArray("medications").toStringList(),
                isOrganDonor = json.optBoolean("isOrganDonor"),
                additionalNotes = json.optString("additionalNotes"),
            )
        }.getOrDefault(MedicalProfile())
    }

    fun saveEmergencyContacts(contacts: List<EmergencyContact>) {
        val array = JSONArray()
        contacts.forEach { contact ->
            array.put(
                JSONObject().apply {
                    put("id", contact.id)
                    put("name", contact.name)
                    put("relationship", contact.relationship)
                    put("phoneNumber", contact.phoneNumber)
                    put("notifyOnSos", contact.notifyOnSos)
                },
            )
        }
        prefs.edit().putString(KEY_CONTACTS, array.toString()).apply()
    }

    fun getEmergencyContacts(): List<EmergencyContact> {
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                EmergencyContact(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    relationship = obj.optString("relationship"),
                    phoneNumber = obj.optString("phoneNumber"),
                    notifyOnSos = obj.optBoolean("notifyOnSos", true),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { getString(it) }
    }

    companion object {
        private const val KEY_MEDICAL = "medical_profile"
        private const val KEY_CONTACTS = "emergency_contacts"
    }
}
