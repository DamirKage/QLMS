# Architecture & design notes

This document explains how the app is put together, and — since the brief
was explicitly "make it better, find the minuses" — what was wrong with the
original 2024 thesis implementation and how each issue was addressed here.

## Stack

- **Kotlin + Jetpack Compose + Material 3.** The original used XML layouts
  with ViewBinding; Compose is the current standard and made the trilingual,
  light/dark, high-contrast requirements far less repetitive to implement.
- **Manual dependency injection** (`core/AppContainer.kt`) instead of
  Hilt/Dagger. At this codebase's size, a DI framework mostly adds
  annotation-processor risk without buying much — `AppContainer` is a
  handful of lazily-built singletons wired once at process start.
- **Firestore**, not Realtime Database. The original used Firebase Realtime
  Database as a single flat JSON tree; Firestore's collections give incidents
  a real schema, support the compound queries the app needs (nearby-by-geohash,
  my-reports-by-uid), and — critically — let security rules be scoped per
  document rather than per arbitrary JSON path.
- **Room + WorkManager** for the offline queue, **DataStore** for settings,
  **EncryptedSharedPreferences** for the local medical/contacts cache.
- **Google Maps Compose** on Android; **Leaflet + OpenStreetMap** on the web
  panel (no API key friction for a panel that's often run from a browser you
  don't control).

## Data model

```
users/{uid}                         profile (name, email, role, language)
users/{uid}/contacts/{contactId}    emergency contacts
incidents/{id}                      PUBLIC: type, status, description,
                                     address, lat/lon, geohash, photos,
                                     isSosTriggered, reporterId, timestamps,
                                     confirmCount, disputeCount
incidents/{id}/private/dispatch     PRIVATE: medicalSnapshot,
                                     contactsSnapshot, dispatcherNote
incidents/{id}/messages/{msgId}     live reporter <-> dispatcher chat
incidents/{id}/verifications/{uid}  one community confirm/dispute vote
                                     per uid — see "Community verification"
trips/{id}                          "walk me home": destination, status,
                                     live lat/lon, expected arrival, contacts
                                     snapshot — owner-only, never part of
                                     the incidents feed (see below)
```

### Why the private subdocument exists

Firestore security rules can grant or deny an entire document — there is no
field-level redaction. The nearby-incidents feed has to be readable by any
signed-in citizen (that's the point of a community safety feed). If medical
info and emergency-contact phone numbers lived on that same document, *every*
citizen browsing the feed could read *every* SOS caller's blood type,
allergies, and family's phone numbers. Splitting sensitive fields into
`incidents/{id}/private/dispatch` — readable only by the reporter and by
`DISPATCHER`/`ADMIN` accounts — is the only way to keep the public feed
genuinely public and the sensitive half genuinely private. See
`firebase/firestore.rules` for the enforcement.

### Why Trip mode is its own collection, not an incident

A "walk me home" trip is not an emergency — most of them end in "arrived
safely" — so it gets none of the incidents feed's shared visibility. `trips`
is locked to its own owner in `firestore.rules` (`isOwner(resource.data.userId)`);
a dispatcher account has no special access to it, and it never appears in
the dispatcher web panel (see the note in the console's incident detail
view). The no-login link a traveler shares with contacts is served the same
way as the incident tracking link — a narrow Cloud Function
(`getPublicTripStatus`) reading one document at a time, never direct
Firestore access — so sharing a trip link can't be turned into browsing
anyone else's trips.

### Why community verification is a subcollection vote, not a public field

`confirmCount`/`disputeCount` are the numbers the feed actually displays,
but they're never written directly by a client — each vote is a transaction
that also writes (or deletes) that voter's own doc at
`incidents/{id}/verifications/{uid}`, so a person can hold at most one vote
per incident and can change their mind. `firestore.rules` restricts a
non-reporter's update to just those two aggregate fields (a reporter can't
vote on their own report), which keeps abuse bounded to "one vote per
account" without needing a moderation backend.

## What was wrong with the original, and what changed

The original thesis document (`ТҮСІНДІРМЕ ЖАЗБА`) is honest that it's a
learning project, and several gaps between its stated requirements and its
actual implementation are visible in its own text:

1. **No incident status model at all.** The original database schema
   (`MainActivity`, `RegisterActivity`, `User`, …) never modeled an incident
   lifecycle — a citizen who reported something had no way to know it was
   ever seen. Fixed: `IncidentStatus` (NEW → ACKNOWLEDGED → DISPATCHED →
   RESOLVED / FALSE_ALARM / CANCELLED) plus a dispatcher web panel to drive it.
2. **"Offline capability" was listed as a non-functional requirement (A.2)
   but never implemented.** Fixed: every report/SOS is written to a local
   Room queue first and synced by WorkManager with exponential backoff —
   see `data/worker/PendingIncidentSyncWorker.kt`.
3. **The user schema required a full IIN** (Individual Identification
   Number — Kazakhstan's national ID number) as a stored profile field.
   That's a serious, unnecessary privacy liability for an app whose whole
   pitch is citizen safety; Firebase Auth already identifies a reporter to a
   dispatcher without it. Removed entirely — see the comment on
   `UserProfile.kt`.
4. **Medical info and emergency contacts had no privacy boundary** in the
   flat-schema design. Fixed via the public/private document split above.
5. **Single language (Kazakh only) despite Kazakhstan being constitutionally
   trilingual** and the thesis's own abstract being written in three
   languages. Fixed: full kk/ru/en localization, `values/`, `values-ru/`,
   `values-en/`.
6. **No real emergency-number integration** — the original never actually
   dialed anything. Fixed: SOS places a real `ACTION_CALL` to Kazakhstan's
   unified **112** line (with per-category fallback to 101/102/103/104),
   falling back to `ACTION_DIAL` if call permission isn't granted.
7. **No push notifications**, despite "хабарландырулар" (incident alerts)
   being listed as a goal. Fixed: FCM + geohash-prefix topics
   (`service/NotificationTopics.kt`), with a reference Cloud Function
   (`functions/index.js`) showing the server-side fan-out.
8. **No account/data deletion path** — a real gap under Kazakhstan's "On
   Personal Data" law, which grants a right to erasure. Fixed: Settings →
   Delete account wipes Firestore data, the local encrypted cache, and the
   Auth account itself.
9. **Feature scope crept toward always-on family location tracking**
   (explicitly compared against Life360 in the original analysis). That's a
   privacy trade-off this app deliberately does *not* make — location is only
   ever captured and shared at the moment of an SOS or a report, never as a
   continuous background feed. This is a scope cut on purpose, not an
   oversight.
10. **UI was plain XML forms with no design system.** Rebuilt in Compose with
    a Material 3 theme, dark mode, and a consistent SOS-red used only for the
    emergency action so it keeps its meaning throughout the app.

## Why SOS still dials 112 instead of only "our own server"

A natural question: since the dispatcher panel already gets an SOS the moment
it's created (a Firestore listener, not a phone call), why not route
*everything* through it instead of also placing a real call? Because a
private app server — however good its live map is — cannot itself dispatch
police, an ambulance, or a fire crew; only Kazakhstan's 112 system can. Making
SOS call "our own server" instead of 112 would look like an upgrade but would
actually be a safety regression: a real emergency would depend on someone
being logged into the dispatcher panel at that moment.

What the dispatcher panel *is* good for — and what several real-world
services (Noonlight, RapidSOS) do — is give a human a live picture of the
situation without waiting on a phone call. Three features built on that idea:

- **Live chat on the incident** (`incidents/{id}/messages`, reporter and
  dispatcher/admin only) — the practical, reliably-buildable stand-in for a
  voice call. A telephony/WebRTC integration would need a real third-party
  account (Twilio/Agora/etc.) and native SDK wiring this environment has no
  way to compile-test; the chat gets most of the same value ("someone
  confirms they've seen this and help is coming") without that risk.
- **No-login live-tracking link** (`web/track.html` + the
  `getPublicIncidentStatus` Cloud Function) — the SOS SMS to emergency
  contacts can include a link that shows only that one incident's live
  location/status, for someone who doesn't have the app. It deliberately
  never talks to Firestore directly from the browser — a public page with
  direct `incidents` read access (even via anonymous auth) could be used to
  scrape every reporter's location, not just the one the link is for; the
  Cloud Function is the only thing that can read Firestore here and returns a
  narrow field subset for exactly one id.
- **Fake incoming call** (`FakeCallActivity`/`FakeCallWorker`) — a discreet
  exit tool, not a dispatcher-facing feature, but the same "real-world safety
  app" category (bSafe, Companion) the other two draw from.

## What's intentionally out of scope

- **Silent SOS (shake) only runs while the app process is alive** (armed from
  `MainActivity`'s lifecycle), not as a persistent background service. A true
  always-on daemon would need a justified special-use foreground service —
  easy to get wrong without a real device to test against, so this ships as
  an honest, smaller feature rather than a background service that might not
  behave as advertised.
- **Per-app language switching asks for a restart** rather than live-recreating
  activities. `AppCompatDelegate.setApplicationLocales` would remove that, but
  the app deliberately doesn't depend on AppCompat's Activity base class.
- **Cloud Functions (`functions/`) are reference code, not auto-deployed.**
  They document the server half of the push-notification feature; wiring them
  up is a `firebase deploy --only functions` away once you have a project.
