package kz.qlms.app.core

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kz.qlms.app.data.local.AppDatabase
import kz.qlms.app.data.local.SecurePrefsManager
import kz.qlms.app.data.local.SettingsDataStore
import kz.qlms.app.data.repository.AlertRepository
import kz.qlms.app.data.repository.AuthRepository
import kz.qlms.app.data.repository.IncidentRepository
import kz.qlms.app.data.repository.TripRepository
import kz.qlms.app.data.repository.UserRepository

/**
 * Hand-written composition root (no Hilt/Dagger). For a codebase this size a
 * DI framework mostly adds annotation-processor risk without buying much —
 * everything below is a handful of singletons wired once at process start.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private val database by lazy { AppDatabase.getInstance(appContext) }
    private val securePrefs by lazy { SecurePrefsManager(appContext) }

    val settingsDataStore by lazy { SettingsDataStore(appContext) }

    val authRepository by lazy { AuthRepository(firebaseAuth) }
    val userRepository by lazy { UserRepository(firestore, securePrefs) }
    val incidentRepository by lazy {
        IncidentRepository(appContext, firestore, storage, database.pendingIncidentDao())
    }
    val tripRepository by lazy { TripRepository(firestore) }
    val alertRepository by lazy { AlertRepository(firestore) }
}
