#!/usr/bin/env node
/**
 * One-off admin script: promotes an existing Firebase Auth user (by email) to
 * DISPATCHER so they can sign into the web panel. Run from the functions/
 * folder after `npm install` there.
 *
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS=./serviceAccountKey.json \
 *     node scripts/set-dispatcher-role.js dispatcher@example.com
 *
 * serviceAccountKey.json comes from Firebase Console -> Project settings ->
 * Service accounts -> Generate new private key. Never commit it (it's in
 * .gitignore already).
 */
const { initializeApp, applicationDefault } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore } = require("firebase-admin/firestore");

const email = process.argv[2];
if (!email) {
  console.error("Usage: node scripts/set-dispatcher-role.js <email> [role]");
  process.exit(1);
}
const role = (process.argv[3] || "DISPATCHER").toUpperCase();
if (!["DISPATCHER", "ADMIN"].includes(role)) {
  console.error(`Invalid role "${role}" — must be DISPATCHER or ADMIN`);
  process.exit(1);
}

initializeApp({ credential: applicationDefault() });

async function main() {
  const user = await getAuth().getUserByEmail(email);
  await getFirestore().collection("users").doc(user.uid).set({ role }, { merge: true });
  console.log(`OK: ${email} (${user.uid}) is now ${role}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
