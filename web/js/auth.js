import { initializeApp } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-app.js";
import { getAuth, signInWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-auth.js";
import { getFirestore, doc, getDoc } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-firestore.js";
import { firebaseConfig, isFirebaseConfigured } from "./firebase-config.js";

const banner = document.getElementById("configBanner");
if (!isFirebaseConfigured()) banner.classList.add("visible");

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

const form = document.getElementById("loginForm");
const errorText = document.getElementById("errorText");
const submitBtn = document.getElementById("submitBtn");

// Only DISPATCHER/ADMIN accounts (see users/{uid}.role) may use this panel —
// a regular citizen's app account authenticates fine but is turned away here,
// enforced client-side for UX and, more importantly, by firestore.rules so it
// can't be bypassed by calling Firestore directly.
const ALLOWED_ROLES = ["DISPATCHER", "ADMIN"];

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  errorText.textContent = "";
  submitBtn.disabled = true;

  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;

  try {
    const credential = await signInWithEmailAndPassword(auth, email, password);
    const userDoc = await getDoc(doc(db, "users", credential.user.uid));
    const role = userDoc.exists() ? userDoc.data().role : null;

    if (!ALLOWED_ROLES.includes(role)) {
      await auth.signOut();
      errorText.textContent = "Бұл аккаунттың диспетчерлік панельге қолжетімділігі жоқ.";
      submitBtn.disabled = false;
      return;
    }

    window.location.href = "index.html";
  } catch (err) {
    errorText.textContent = "Кіру мүмкін болмады: электрондық пошта немесе құпия сөз қате.";
    submitBtn.disabled = false;
  }
});
