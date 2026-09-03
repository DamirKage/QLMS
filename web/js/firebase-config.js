// Fill these in from your Firebase project settings (Project settings ->
// General -> "Your apps" -> Web app). This file intentionally ships with
// placeholder values — the panel will show a clear "not configured" banner
// instead of a cryptic Firebase error until you replace them.
// See ../docs/SETUP.md for the full walkthrough.
export const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT_ID.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID",
};

export const isFirebaseConfigured = () => firebaseConfig.apiKey !== "YOUR_API_KEY";
