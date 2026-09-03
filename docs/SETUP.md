# Setup guide

This repo ships no real API keys or Firebase project — you need your own.
Everything below is one-time setup; after that, iterating is just editing
code and re-running.

## 1. Prerequisites

- JDK 17 (Android Gradle Plugin 8.5 requires it)
- Node.js 18+ (only needed for the Firebase CLI / Cloud Functions)
- A Google account (for Firebase + Google Cloud Console)
- One of: Android Studio, IntelliJ IDEA (+ Android plugin), or VS Code +
  command-line Android SDK tools (see §3)

## 2. Create the Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com) → **Add project**.
2. Once created, click **Add app → Android**. Package name: `kz.qlms.app`
   (must match `applicationId` in `app/build.gradle.kts` exactly, including
   the `.debug` suffix Gradle appends to debug builds — for local testing add
   `kz.qlms.app.debug` as a *second* Android app in the same Firebase project,
   or just build a release-config APK; either works, the debug suffix is the
   one common gotcha here).
3. Download the generated **`google-services.json`** and place it at
   `app/google-services.json` (already gitignored — never commit the real one).
4. In the console, enable:
   - **Authentication → Sign-in method → Email/Password**
   - **Firestore Database** (start in production mode — the rules in this
     repo replace the defaults, see §5)
   - **Storage**
   - **Cloud Messaging** (no setup needed beyond having a project)

## 3. Open and build the Android app

**Option A — Android Studio (simplest).** Open the repo root, let Gradle sync,
run the `app` configuration on an emulator or a USB-debugging device.

**Option B — IntelliJ IDEA.** Install the **Android** plugin (Settings →
Plugins → Marketplace; bundled in Ultimate), restart, then **File → Open**
the repo root. First sync will offer to install an Android SDK if you don't
have one. After sync, a device dropdown + Run button appear just like Android
Studio.

**Option C — VS Code + command-line SDK.** VS Code has no built-in Android
run/deploy support, but you can edit here and build/run from a terminal:

1. Install the [command-line Android SDK tools](https://developer.android.com/studio#command-tools)
   (a small zip, no full IDE) or point `sdk.dir` in `local.properties` at an
   SDK installed by Android Studio/IntelliJ elsewhere on the machine.
2. `sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"`
3. Create `local.properties` (see §6) with `sdk.dir=/path/to/Android/Sdk`.
4. Build: `./gradlew assembleDebug`
5. Install on a connected device/emulator: `./gradlew installDebug`, then
   launch it with `adb shell am start -n kz.qlms.app.debug/kz.qlms.app.MainActivity`.
   For an emulator without Android Studio, `avdmanager`/`emulator` from the
   command-line tools work the same way — see Google's docs linked above.
6. The Kotlin and XML language servers in VS Code (via the "Kotlin" extension)
   give you syntax highlighting and basic completion, but not the deep
   refactoring/inspection support of an IntelliJ-based IDE — fine for editing,
   not a substitute for A/B above if you're doing heavy Compose UI work.

## 4. Google Maps API key

1. [Google Cloud Console](https://console.cloud.google.com) → select the
   **same project** Firebase created → **APIs & Services → Library** → enable
   **"Maps SDK for Android"**.
2. **APIs & Services → Credentials → Create credentials → API key.**
   Restrict it to "Android apps" and add your package name (`kz.qlms.app` /
   `kz.qlms.app.debug`) + SHA-1 (get it with
   `./gradlew signingReport` for the debug keystore).
3. Copy `local.properties.example` to `local.properties` and set
   `MAPS_API_KEY=<your key>`.

## 5. Deploy Firestore/Storage rules and indexes

```bash
npm install -g firebase-tools     # once
firebase login
firebase use --add                # pick your project, alias it "default"
firebase deploy --only firestore:rules,firestore:indexes,storage
```

This pushes `firebase/firestore.rules`, `firebase/firestore.indexes.json`,
and `firebase/storage.rules` — the app will not work correctly against the
Firestore *default* rules (which block everything).

## 6. `local.properties`

```
sdk.dir=/path/to/Android/Sdk
MAPS_API_KEY=your_maps_key_here
TRACKING_BASE_URL=https://your-project.web.app   # optional, see §7 and §8
```

This file is gitignored — every developer/machine has their own.

## 7. Run the dispatcher web panel

1. Edit `web/js/firebase-config.js` with your Firebase project's web config
   (Firebase Console → Project settings → General → "Your apps" → Web app —
   create one if you don't have it yet, the `</>` icon).
2. Locally: `firebase serve --only hosting` (serves `web/` at
   `http://localhost:5000`), or just open `web/login.html` in a browser —
   both work since there's no build step.
3. Deploy for real: `firebase deploy --only hosting`.
4. **Create a dispatcher account.** Sign up through the Android app (or
   Firebase Console → Authentication → Add user) with the dispatcher's email,
   then promote their role:
   ```bash
   cd functions && npm install
   GOOGLE_APPLICATION_CREDENTIALS=./serviceAccountKey.json \
     node scripts/set-dispatcher-role.js dispatcher@example.com
   ```
   (`serviceAccountKey.json` from Firebase Console → Project settings →
   Service accounts → Generate new private key — gitignored, never commit it.)
   Or just edit the `users/{uid}` document's `role` field to `"DISPATCHER"`
   directly in the Firestore console for a one-off test account.

## 8. (Optional) Deploy the reference Cloud Functions

```bash
cd functions && npm install
firebase deploy --only functions
```

This wires up "new incident near you" and "your report status changed" push
notifications, plus `getPublicIncidentStatus`, which backs the no-login
live-tracking link (`web/track.html`) that can ride along in the SOS SMS to
emergency contacts. That page deliberately never talks to Firestore directly
— only this function can read it, and it returns a narrow field subset for
one incident id at a time, so the link can't be used to browse anyone else's
reports. After deploying, set `TRACKING_BASE_URL` in `local.properties` (§6)
to your Hosting domain and rebuild the app for the link to start appearing
in SOS messages. None of this is required for the core SOS/report/dispatch
flow to work.

## 9. Testing the SOS flow end to end

1. Run the Android app on a device (an emulator can fake GPS via Extended
   Controls → Location, but real permission prompts and phone calls are best
   tested on a physical device).
2. Register an account, grant location permission when prompted.
3. Optionally add a medical profile / emergency contact under Profile.
4. Tap **SOS**, pick a type, let the countdown finish (or cancel it to see
   that path too).
5. Open the dispatcher web panel signed in as your dispatcher account — the
   incident should appear on the map and in the list within a couple of
   seconds, with the medical/contacts panel visible only there and to the
   reporter.
