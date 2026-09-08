/**
 * Reference Cloud Functions for QLMS (Firebase Functions v2, Node 20).
 *
 * These are NOT auto-deployed by this repo — they document the server-side
 * half of two features the Android client already assumes exist:
 *
 *   1. onIncidentCreated — fans out a push notification to everyone subscribed
 *      to the incident's geohash-area topic (see NotificationTopics.kt on the
 *      client) so nearby users get a "new incident near you" alert.
 *
 *   2. onIncidentUpdated — when a dispatcher (web panel) changes an incident's
 *      status, pushes a notification straight to the reporter.
 *
 *   3. getPublicIncidentStatus — backs web/track.html, the no-login link sent
 *      to emergency contacts by SMS. It deliberately does NOT give the page
 *      direct Firestore access: a public page with `incidents` read access
 *      (even via anonymous auth) could list/scrape every reporter's location
 *      in the whole collection, not just the one incident the link is for.
 *      This function is the only thing that can read Firestore here (Admin
 *      SDK bypasses rules) and it returns a deliberately narrow field subset
 *      for exactly one id — no reporter identity, no description text.
 *
 *   4. getPublicTripStatus — the same narrow-read pattern as (3), for
 *      web/trip.html, the link a traveler shares from Trip mode. The
 *      `trips` Firestore rule (see firebase/firestore.rules) locks the
 *      collection to its owner only, so this function — not a rule change —
 *      is what lets someone holding the link see live progress without
 *      signing in and without being able to browse anyone else's trip.
 *
 *   5. onAlertCreated — the reverse-112/Wireless-Emergency-Alerts idea: when
 *      a dispatcher publishes an `alerts/{id}` doc, fans it out to every
 *      geohash-area topic covering its (center, radius), reusing the exact
 *      topic scheme NotificationTopics.kt already subscribes clients to for
 *      "incident near you" pushes — no new subscription mechanism needed.
 *      This is a deliberately bounded MVP: topics are ~20km square cells,
 *      not a true geodesic circle, so a device can receive the push slightly
 *      outside the alert's radius — SafetyAlert.isRelevantTo() on the client
 *      does the precise circular check before showing an in-app banner. A
 *      real carrier-level WEA broadcasts over the cell network itself; an
 *      app can only ever reach devices that already have it installed and
 *      subscribed, which is the real (unavoidable) limitation here.
 *
 * Deploy with: firebase deploy --only functions   (after `firebase init functions`
 * in this folder and filling in a real Firebase project).
 */

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { onRequest } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

const AREA_TOPIC_PREFIX = "area_";
const GEOHASH_TOPIC_PRECISION = 4;

exports.onIncidentCreated = onDocumentCreated("incidents/{incidentId}", async (event) => {
  const incident = event.data?.data();
  if (!incident || !incident.geohash) return;

  const topic = AREA_TOPIC_PREFIX + incident.geohash.substring(0, GEOHASH_TOPIC_PRECISION);
  const title = incident.isSosTriggered ? "SOS near you" : "New report near you";

  await getMessaging().send({
    topic,
    notification: {
      title,
      body: incident.description || incident.type || "Tap for details",
    },
    data: { incidentId: event.params.incidentId },
  });
});

exports.onIncidentUpdated = onDocumentUpdated("incidents/{incidentId}", async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  if (!before || !after || before.status === after.status) return;
  if (!after.reporterId) return;

  // In production, resolve the reporter's FCM token from users/{uid}/tokens
  // instead of a topic — omitted here since token storage isn't wired up on
  // the client (see QlmsFirebaseMessagingService's docstring).
  await getMessaging().send({
    topic: `user_${after.reporterId}`,
    notification: {
      title: "Your report status changed",
      body: `Status: ${after.status}`,
    },
    data: { incidentId: event.params.incidentId },
  });
});

const PUBLIC_FIELDS = ["type", "status", "latitude", "longitude", "isSosTriggered", "updatedAt", "createdAt"];

exports.getPublicIncidentStatus = onRequest({ cors: true }, async (req, res) => {
  const id = req.query.id;
  if (!id || typeof id !== "string") {
    res.status(400).json({ error: "missing id" });
    return;
  }

  const snap = await getFirestore().collection("incidents").doc(id).get();
  if (!snap.exists) {
    res.status(404).json({ error: "not found" });
    return;
  }

  const data = snap.data();
  const publicView = {};
  for (const field of PUBLIC_FIELDS) {
    if (data[field] !== undefined) publicView[field] = data[field];
  }
  res.status(200).json(publicView);
});

const PUBLIC_TRIP_FIELDS = ["destination", "statusName", "latitude", "longitude", "expectedArrivalAtEpochMs", "updatedAt"];

exports.getPublicTripStatus = onRequest({ cors: true }, async (req, res) => {
  const id = req.query.id;
  if (!id || typeof id !== "string") {
    res.status(400).json({ error: "missing id" });
    return;
  }

  const snap = await getFirestore().collection("trips").doc(id).get();
  if (!snap.exists) {
    res.status(404).json({ error: "not found" });
    return;
  }

  const data = snap.data();
  const publicView = {};
  for (const field of PUBLIC_TRIP_FIELDS) {
    if (data[field] !== undefined) publicView[field] = data[field];
  }
  res.status(200).json(publicView);
});

// Same bit-interleaving geohash algorithm as GeoHash.kt on the client, ported
// to JS so this function computes exactly the topic names clients actually
// subscribe to — see NotificationTopics.kt (PREFIX_PRECISION = 4).
const GEOHASH_BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

function encodeGeohash(latitude, longitude, precision) {
  let latMin = -90, latMax = 90;
  let lonMin = -180, lonMax = 180;
  let hash = "";
  let isEven = true;
  let bit = 0;
  let ch = 0;
  while (hash.length < precision) {
    if (isEven) {
      const mid = (lonMin + lonMax) / 2;
      if (longitude > mid) { ch |= (1 << (4 - bit)); lonMin = mid; } else { lonMax = mid; }
    } else {
      const mid = (latMin + latMax) / 2;
      if (latitude > mid) { ch |= (1 << (4 - bit)); latMin = mid; } else { latMax = mid; }
    }
    isEven = !isEven;
    if (bit < 4) {
      bit++;
    } else {
      hash += GEOHASH_BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }
  return hash;
}

const ALERT_CELL_KM = 20; // matches NotificationTopics.kt's precision-4 (~20km) topic cells

/** Every precision-4 topic cell that could hold a device within radiusKm of (latitude, longitude). */
function areaTopicsForAlert(latitude, longitude, radiusKm) {
  const steps = Math.max(1, Math.ceil(radiusKm / ALERT_CELL_KM));
  const latStep = ALERT_CELL_KM / 111.0;
  const lonStep = ALERT_CELL_KM / (111.0 * Math.max(0.2, Math.cos((latitude * Math.PI) / 180)));
  const topics = new Set();
  for (let dLat = -steps; dLat <= steps; dLat++) {
    for (let dLon = -steps; dLon <= steps; dLon++) {
      const lat = Math.min(90, Math.max(-90, latitude + dLat * latStep));
      const lon = Math.min(180, Math.max(-180, longitude + dLon * lonStep));
      topics.add(AREA_TOPIC_PREFIX + encodeGeohash(lat, lon, GEOHASH_TOPIC_PRECISION));
    }
  }
  return Array.from(topics);
}

exports.onAlertCreated = onDocumentCreated("alerts/{alertId}", async (event) => {
  const alert = event.data?.data();
  if (!alert || alert.latitude === undefined || alert.longitude === undefined) return;

  const topics = areaTopicsForAlert(alert.latitude, alert.longitude, alert.radiusKm || 5);
  await Promise.all(
    topics.map((topic) =>
      getMessaging()
        .send({
          topic,
          notification: { title: alert.title, body: alert.body },
          data: { type: "area_alert", alertId: event.params.alertId },
        })
        // A topic with zero current subscribers still sends fine; only a
        // malformed topic name would reject, so one bad cell shouldn't sink
        // delivery to every other cell in the fan-out.
        .catch((err) => console.error(`onAlertCreated: failed to send to ${topic}`, err)),
    ),
  );
});
