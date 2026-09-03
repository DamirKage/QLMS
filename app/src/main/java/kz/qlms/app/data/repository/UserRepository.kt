package kz.qlms.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kz.qlms.app.core.QlmsResult
import kz.qlms.app.data.local.SecurePrefsManager
import kz.qlms.app.data.model.EmergencyContact
import kz.qlms.app.data.model.MedicalProfile
import kz.qlms.app.data.model.UserProfile
import kz.qlms.app.data.remote.FirestoreSchema

class UserRepository(
    private val firestore: FirebaseFirestore,
    private val securePrefs: SecurePrefsManager,
) {
    private fun userDoc(uid: String) = firestore.collection(FirestoreSchema.USERS).document(uid)

    suspend fun createProfileIfMissing(uid: String, fullName: String, email: String): QlmsResult<Unit> = runCatching {
        val doc = userDoc(uid).get().await()
        if (!doc.exists()) {
            userDoc(uid).set(UserProfile(uid = uid, fullName = fullName, email = email)).await()
        }
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not create profile", it) },
    )

    suspend fun getProfile(uid: String): QlmsResult<UserProfile> = runCatching {
        userDoc(uid).get().await().toObject(UserProfile::class.java)?.copy(uid = uid)
            ?: UserProfile(uid = uid)
    }.fold(
        onSuccess = { QlmsResult.Success(it) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not load profile", it) },
    )

    suspend fun updateProfile(profile: UserProfile): QlmsResult<Unit> = runCatching {
        userDoc(profile.uid).set(profile).await()
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not update profile", it) },
    )

    // --- Medical info: cached locally (encrypted) so it's available even offline. ---

    fun getCachedMedicalProfile(): MedicalProfile = securePrefs.getMedicalProfile()

    suspend fun saveMedicalProfile(uid: String, profile: MedicalProfile): QlmsResult<Unit> {
        securePrefs.saveMedicalProfile(profile)
        return runCatching {
            userDoc(uid).set(mapOf(FirestoreSchema.UserFields.MEDICAL_PROFILE to profile), com.google.firebase.firestore.SetOptions.merge()).await()
        }.fold(
            onSuccess = { QlmsResult.Success(Unit) },
            onFailure = { QlmsResult.Error(it.message ?: "Saved locally, will sync later", it) },
        )
    }

    // --- Emergency contacts: same local-first pattern. ---

    fun getCachedContacts(): List<EmergencyContact> = securePrefs.getEmergencyContacts()

    suspend fun saveContacts(uid: String, contacts: List<EmergencyContact>): QlmsResult<Unit> {
        securePrefs.saveEmergencyContacts(contacts)
        return runCatching {
            userDoc(uid).collection(FirestoreSchema.UserFields.CONTACTS).get().await().documents
                .forEach { it.reference.delete() }
            contacts.forEach { contact ->
                userDoc(uid).collection(FirestoreSchema.UserFields.CONTACTS).document(contact.id).set(contact).await()
            }
        }.fold(
            onSuccess = { QlmsResult.Success(Unit) },
            onFailure = { QlmsResult.Error(it.message ?: "Saved locally, will sync later", it) },
        )
    }

    /** Deletes the Firestore profile + contacts subcollection and wipes the local encrypted cache. Call before AuthRepository.deleteAccount(). */
    suspend fun deleteAllUserData(uid: String): QlmsResult<Unit> = runCatching {
        userDoc(uid).collection(FirestoreSchema.UserFields.CONTACTS).get().await().documents.forEach { it.reference.delete() }
        userDoc(uid).delete().await()
        securePrefs.saveMedicalProfile(MedicalProfile())
        securePrefs.saveEmergencyContacts(emptyList())
    }.fold(
        onSuccess = { QlmsResult.Success(Unit) },
        onFailure = { QlmsResult.Error(it.message ?: "Could not delete data", it) },
    )
}
