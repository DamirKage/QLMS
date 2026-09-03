# QLMS — Қылмыс пен төтенше жағдайлардың алдын алу қосымшасы

QLMS is a public-safety platform for Kazakhstan built around one idea: when
something goes wrong, getting help should take one tap, not a fumbled phone
call. It started as a Satbayev University diploma project (Тлеугажиев Д. Б.,
Хабибуллин А. Б., "Development of a geolocation-based rapid-response system
for crime and emergency prevention (QLMS)", 2024) and this repository is a
ground-up rebuild of it: same mission, current Android stack, and every gap
between what the original thesis promised and what it actually shipped
closed. See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the detailed
before/after.

## What's here

| Part | Stack | Where |
|---|---|---|
| **Android app** (citizens) | Kotlin, Jetpack Compose, Material 3, Firebase, Google Maps | `app/` |
| **Dispatcher web panel** | Plain HTML/CSS/JS, Firebase, Leaflet | `web/` |
| **Cloud Functions** (reference push fan-out) | Node.js | `functions/` |
| **Security rules & indexes** | Firestore / Storage rules | `firebase/` |

## Core features (Android app)

- **SOS button** — one tap, a cancelable countdown, then: your location is
  captured, an incident is created, your chosen emergency contacts get an SMS
  with a map link, and the phone dials Kazakhstan's unified **112** emergency
  line (falling back to `101`/`102`/`103`/`104` per incident type when useful).
- **Incident reporting** — crime, fire, medical, road accident, domestic
  violence, missing person, natural disaster, gas leak, or other — with a
  description, address, and photos, anonymously if you choose.
- **Medical profile & emergency contacts** — blood type, allergies, chronic
  conditions, medications; shared only at the moment you trigger SOS, never
  browsable by anyone else (see the privacy section below).
- **Nearby incident feed & map** — see what's been reported around you.
- **Safety check-in** — arm a timer; if you don't confirm "I'm safe" in time,
  your emergency contacts are notified automatically.
- **Silent SOS** — three hard shakes trigger SOS without touching the screen.
- **Offline-first SOS/reports** — a report made with no signal is queued on
  the device and delivered the moment connectivity returns.
- **Trilingual** — Kazakh, Russian, English (Kazakhstan is a trilingual state;
  the original thesis only ever shipped one language).
- **Right to erasure** — delete your account and every piece of stored data
  in one action, per Kazakhstan's "On Personal Data" law.

## Dispatcher web panel

A live console for police/dispatcher accounts: a real-time incident map,
list with status filters, per-incident detail (medical info + emergency
contacts, visible only here and to the reporter), and a status workflow
(New → Acknowledged → Dispatched → Resolved / False alarm).

## Getting started

You need a Firebase project of your own (this repo ships no real keys) and
either Android Studio, IntelliJ IDEA with the Android plugin, or VS Code +
the command-line Android SDK. Full step-by-step setup — creating the Firebase
project, wiring `google-services.json`, getting a Maps API key, deploying
Firestore rules, running the web panel, promoting a dispatcher account — is
in [`docs/SETUP.md`](docs/SETUP.md).

## Repository layout

```
app/                Android app (Kotlin, Jetpack Compose)
web/                 Dispatcher web panel
functions/           Reference Cloud Functions + admin scripts
firebase/            Firestore/Storage security rules, indexes
docs/                Architecture notes and setup guide
```

## License / attribution

Academic/portfolio project. Built with [Claude Code](https://claude.com/claude-code).
