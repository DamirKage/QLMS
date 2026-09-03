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
 * Deploy with: firebase deploy --only functions   (after `firebase init functions`
 * in this folder and filling in a real Firebase project).
 */

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

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
