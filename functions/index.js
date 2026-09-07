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
