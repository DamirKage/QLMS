# Market research — what exists, what QLMS does differently

Researched September 2026. This isn't a generic "look at competitors" exercise —
each row below turned into an actual feature decision (or a deliberate
non-feature) in the codebase, referenced in the right column.

## The competitive landscape

| Product | Model | Strength | Real weakness | What QLMS does about it |
|---|---|---|---|---|
| **Kazakhstan's official "112 — Экстренная помощь"** ([App Store](https://apps.apple.com/kz/app/112-%D1%8D%D0%BA%D1%81%D1%82%D1%80%D0%B5%D0%BD%D0%BD%D0%B0%D1%8F-%D0%BF%D0%BE%D0%BC%D0%BE%D1%89%D1%8C/id1499805544), [112service.app](https://112service.app/)) | Government reactive dispatch | Direct pipe to real dispatchers, sends profile/geolocation/photo/audio/video with the call; has a "Поход" (Hiking) mode — register a route, a return-by time, and contacts | No community layer at all (purely 1:1 with dispatch), no live two-way chat back to the reporter, no social verification, check-in mode is hiking-only | We're the layer *on top* of the state system, not a replacement for it: SOS still dials the real **112** ([`SosForegroundService.kt`](../app/src/main/java/kz/qlms/app/service/SosForegroundService.kt)). We generalized "Поход" into **Trip mode** (any activity, not just hiking), added live two-way **dispatcher chat**, and a **community feed** the state app has none of. |
| **Noonlight / SafeTrek** ([bestapp.com](https://www.bestapp.com/best-personal-safety-apps/)) | Hold-button, PIN-to-cancel | Fast, low-friction arm gesture; genuinely excellent US dispatch integration | US-only, entirely reactive, $5–10/mo for the good parts | Added **hold-to-arm** as an alternative trigger mode alongside tap+countdown — see `HoldToArmController`. |
| **bSafe** ([bestapp.com](https://www.bestapp.com/best-personal-safety-apps/)) | Guardian network, live audio/video, timed check-in | Best free tier of the category; audio/video streamed to guardians during SOS; self-firing check-in timer | Streaming needs a live connection the whole time; guardians must also run the app | We do **evidence recording** attached to the incident (works even if a guardian is offline — they get it once synced) rather than requiring a live stream, and our check-in/Trip mode contacts need **no app at all** (SMS + a no-login tracking web page). |
| **Life360** ([bestapp.com](https://www.bestapp.com/best-personal-safety-apps/)) | Continuous family tracking + crash detection | Polished, reliable, real crash detection | "Always-on tracking is a privacy price many independent adults simply refuse to pay" — it's built for parents tracking teens, not adults | We kept our stance from the original build: **no continuous background tracking, ever**. Crash detection is opt-in and only activates a bounded SOS flow at the moment of an actual detected impact — see `CrashDetector`. |
| **Citizen** ([edgeorbital.io](https://www.edgeorbital.io/2026/05/31/is-citizen-app-safe-campus-safety-privacy-2026/), [contrary.com](https://research.contrary.com/company/citizen)) | Crowd-sourced crime map + live video | Real-time nearby-incident awareness people actually want | Its police data partnership ended June 2026 and safety-critical features moved behind a paywall — the community layer lost both credibility and access when the company needed revenue | Our nearby feed stays free with **no paywall on safety features** (monetization, if ever, stays away from anything safety-critical — see `docs/ARCHITECTURE.md`), and we address Citizen's credibility problem directly with **community verification** (confirm / mark false alarm) rather than trusting every report at face value. |
| **RapidSOS** ([rapidsos.com](https://rapidsos.com/enterprise/home-and-personal-safety/), [prnewswire.com](https://www.prnewswire.com/news-releases/google-and-rapidsos-enable-emergency-live-video-on-android-for-911-302637385.html)) | B2B: feeds rich caller data straight into real 911 CAD systems | Genuinely closes the "dispatcher doesn't know where you are" gap at the infrastructure level | Requires direct CAD integration deals with each PSAP — out of reach for an independent app | Out of scope for a standalone app (needs government partnership we don't have), but the *shape* of the idea — get precise location + profile data in front of a human fast — is exactly what our SOS → Firestore → dispatcher-panel pipeline already does at a smaller scale, plus the literal 112 call for the parts only the state system can actually dispatch. |

## Net feature set added after this research

1. **Hold-to-arm trigger mode** (`HoldToArmController` / Settings) — press-and-hold instead of tap+countdown, PIN-to-cancel.
2. **Panic siren + flashlight strobe** (`SirenController`) — disorient/attract-attention tool during active SOS.
3. **Trip / Walk-me-home mode** (`ui/trip/*`, `data/model/Trip.kt`) — generalizes the state app's hiking-only check-in to any activity, with a live no-login tracking link.
4. **Community verification** (`incidents/{id}/votes`) — confirm or flag-false-alarm on nearby reports, addressing the credibility problem that broke Citizen's model.
5. **Crash/fall auto-detection** (`CrashDetector`) — opt-in, bounded, never continuous tracking.

## What we deliberately did not build

- **Continuous location tracking** (Life360's core model) — a privacy trade-off we reject on principle, not a missing feature.
- **Live video/audio streaming during SOS** (bSafe's approach) — needs a signaling/relay server and a paid TURN service to work reliably; we do evidence **recording** instead, which needs no server-side infra and still gets responders the material afterward.
- **A payments/paywall layer on safety features** (Citizen's 2026 pivot) — the lesson from Citizen's backlash is directly why core safety features here stay free.
