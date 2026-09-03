import { initializeApp } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-app.js";
import { getAuth, onAuthStateChanged, signOut } from "https://www.gstatic.com/firebasejs/10.13.0/firebase-auth.js";
import {
  getFirestore, doc, getDoc, collection, query, orderBy, limit,
  onSnapshot, updateDoc, setDoc, serverTimestamp,
} from "https://www.gstatic.com/firebasejs/10.13.0/firebase-firestore.js";
import { firebaseConfig, isFirebaseConfigured } from "./firebase-config.js";

document.getElementById("configBanner").classList.toggle("visible", !isFirebaseConfigured());

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

const ALLOWED_ROLES = ["DISPATCHER", "ADMIN"];

const TYPE_LABELS = {
  CRIME: "Преступление", FIRE: "Пожар", MEDICAL: "Медицинский случай",
  ROAD_ACCIDENT: "ДТП", DOMESTIC_VIOLENCE: "Бытовое насилие",
  MISSING_PERSON: "Пропавший человек", NATURAL_DISASTER: "Стихийное бедствие",
  GAS_LEAK: "Утечка газа", OTHER: "Другое",
};
const STATUS_LABELS = {
  NEW: "Новое", ACKNOWLEDGED: "Принято", DISPATCHED: "Направлено",
  RESOLVED: "Решено", FALSE_ALARM: "Ложная тревога", CANCELLED: "Отменено",
};
const TYPE_ICONS = {
  CRIME: "🚨", FIRE: "🔥", MEDICAL: "🩺", ROAD_ACCIDENT: "🚗",
  DOMESTIC_VIOLENCE: "🏠", MISSING_PERSON: "🔍", NATURAL_DISASTER: "⚠️",
  GAS_LEAK: "💨", OTHER: "❓",
};

let incidents = [];
let selectedId = null;
let activeFilter = "ALL";
let privateUnsub = null;

// ---------- Auth guard ----------
onAuthStateChanged(auth, async (user) => {
  if (!user) {
    window.location.href = "login.html";
    return;
  }
  const userDoc = await getDoc(doc(db, "users", user.uid));
  const role = userDoc.exists() ? userDoc.data().role : null;
  if (!ALLOWED_ROLES.includes(role)) {
    await signOut(auth);
    window.location.href = "login.html";
    return;
  }
  document.getElementById("userEmail").textContent = user.email ?? "";
  startDashboard();
});

document.getElementById("signOutBtn").addEventListener("click", () => signOut(auth));

// ---------- Map ----------
const map = L.map("map").setView([43.238949, 76.889709], 12);
L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
  attribution: "&copy; OpenStreetMap contributors",
  maxZoom: 19,
}).addTo(map);

const markers = new Map();

function markerColor(incident) {
  if (incident.isSosTriggered) return "#e0301f";
  if (incident.status === "RESOLVED") return "#1e7a34";
  return "#0b4da6";
}

function renderMarkers() {
  const seen = new Set();
  incidents.forEach((incident) => {
    if (!incident.latitude || !incident.longitude) return;
    seen.add(incident.id);
    const color = markerColor(incident);
    if (markers.has(incident.id)) {
      const marker = markers.get(incident.id);
      marker.setLatLng([incident.latitude, incident.longitude]);
      marker.setStyle({ fillColor: color, color });
    } else {
      const marker = L.circleMarker([incident.latitude, incident.longitude], {
        radius: incident.isSosTriggered ? 10 : 7,
        fillColor: color, color, weight: 2, fillOpacity: 0.85,
      }).addTo(map);
      marker.on("click", () => selectIncident(incident.id));
      markers.set(incident.id, marker);
    }
  });
  for (const [id, marker] of markers.entries()) {
    if (!seen.has(id)) { map.removeLayer(marker); markers.delete(id); }
  }
}

// ---------- Dashboard ----------
function startDashboard() {
  const q = query(collection(db, "incidents"), orderBy("createdAt", "desc"), limit(300));
  onSnapshot(q, (snapshot) => {
    incidents = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }));
    renderStats();
    renderList();
    renderMarkers();
    if (selectedId) renderDetail(incidents.find((i) => i.id === selectedId) ?? null);
  });

  document.getElementById("filterBar").addEventListener("click", (event) => {
    const chip = event.target.closest(".filter-chip");
    if (!chip) return;
    activeFilter = chip.dataset.filter;
    document.querySelectorAll(".filter-chip").forEach((c) => c.classList.toggle("active", c === chip));
    renderList();
  });
}

function renderStats() {
  const dayAgo = Date.now() - 24 * 60 * 60 * 1000;
  const recent = incidents.filter((i) => toMillis(i.createdAt) >= dayAgo);
  document.getElementById("statNew").textContent = incidents.filter((i) => i.status === "NEW").length;
  document.getElementById("statProgress").textContent = incidents.filter((i) => ["ACKNOWLEDGED", "DISPATCHED"].includes(i.status)).length;
  document.getElementById("statResolved").textContent = incidents.filter((i) => i.status === "RESOLVED").length;
  document.getElementById("statTotal").textContent = recent.length;
}

function toMillis(ts) {
  if (!ts) return 0;
  return typeof ts.toMillis === "function" ? ts.toMillis() : 0;
}

function renderList() {
  const container = document.getElementById("incidentList");
  const filtered = activeFilter === "ALL" ? incidents : incidents.filter((i) => i.status === activeFilter);

  if (filtered.length === 0) {
    container.innerHTML = '<div class="empty-hint">Оқиғалар жоқ</div>';
    return;
  }

  container.innerHTML = filtered.map((incident) => `
    <div class="incident-item ${incident.isSosTriggered ? "sos" : ""} ${incident.id === selectedId ? "selected" : ""}" data-id="${incident.id}">
      <div class="icon">${TYPE_ICONS[incident.type] ?? "❓"}</div>
      <div>
        <div class="title">${escapeHtml(TYPE_LABELS[incident.type] ?? incident.type)} ${incident.isSosTriggered ? "· SOS" : ""}</div>
        <div class="meta">${escapeHtml(incident.address || formatCoords(incident))}</div>
        <div class="meta">${formatTime(incident.createdAt)}</div>
        <span class="badge ${incident.status}">${STATUS_LABELS[incident.status] ?? incident.status}</span>
      </div>
    </div>
  `).join("");

  container.querySelectorAll(".incident-item").forEach((el) => {
    el.addEventListener("click", () => selectIncident(el.dataset.id));
  });
}

function selectIncident(id) {
  selectedId = id;
  renderList();
  const incident = incidents.find((i) => i.id === id);
  if (incident && incident.latitude) map.panTo([incident.latitude, incident.longitude]);
  renderDetail(incident ?? null);

  if (privateUnsub) { privateUnsub(); privateUnsub = null; }
  if (id) {
    privateUnsub = onSnapshot(
      doc(db, "incidents", id, "private", "dispatch"),
      (snap) => renderPrivateSection(snap.exists() ? snap.data() : null),
      () => renderPrivateSection(null, true),
    );
  }
}

function renderDetail(incident) {
  const panel = document.getElementById("detailPanel");
  if (!incident) {
    panel.innerHTML = '<div class="empty-hint">Тізімнен оқиғаны таңдаңыз</div>';
    return;
  }
  panel.innerHTML = `
    <h2>${escapeHtml(TYPE_LABELS[incident.type] ?? incident.type)}</h2>
    <span class="badge ${incident.status}">${STATUS_LABELS[incident.status] ?? incident.status}</span>

    <div class="section">
      <h3>Сипаттама</h3>
      <div class="detail-row">${escapeHtml(incident.description || "—")}</div>
    </div>
    <div class="section">
      <h3>Орын</h3>
      <div class="detail-row"><span class="k">Мекенжай:</span> ${escapeHtml(incident.address || "—")}</div>
      <div class="detail-row"><span class="k">Координаттар:</span> ${formatCoords(incident)}</div>
    </div>
    <div class="section" id="privateSection">
      <h3>Құпия деректер</h3>
      <div class="private-locked">Жүктелуде…</div>
    </div>
    <div class="section">
      <h3>Мәртебені өзгерту</h3>
      <div class="status-actions">
        <button data-status="ACKNOWLEDGED">Қабылдау</button>
        <button data-status="DISPATCHED">Жіберілді деп белгілеу</button>
        <button data-status="RESOLVED">Шешілді</button>
        <button data-status="FALSE_ALARM" class="danger">Жалған дабыл</button>
      </div>
    </div>
    <div class="section">
      <h3>Диспетчер жазбасы</h3>
      <textarea id="noteInput" rows="3" placeholder="Ішкі жазба…"></textarea>
      <div class="status-actions"><button id="saveNoteBtn">Жазбаны сақтау</button></div>
    </div>
  `;

  panel.querySelectorAll(".status-actions button[data-status]").forEach((btn) => {
    btn.addEventListener("click", () => updateStatus(incident.id, btn.dataset.status));
  });
  const saveNoteBtn = document.getElementById("saveNoteBtn");
  if (saveNoteBtn) saveNoteBtn.addEventListener("click", () => saveNote(incident.id));
}

function renderPrivateSection(data, denied = false) {
  const section = document.getElementById("privateSection");
  if (!section) return;
  if (denied || !data) {
    section.innerHTML = `<h3>Құпия деректер</h3><div class="private-locked">Медициналық ақпарат немесе байланыстар қосылмаған.</div>`;
    return;
  }
  const medical = data.medicalSnapshot;
  const contacts = data.contactsSnapshot ?? [];
  const noteInput = document.getElementById("noteInput");
  if (noteInput && data.dispatcherNote) noteInput.value = data.dispatcherNote;

  section.innerHTML = `
    <h3>Медициналық ақпарат</h3>
    ${medical ? `
      <div class="detail-row"><span class="k">Қан тобы:</span> ${escapeHtml(medical.bloodType || "—")}</div>
      <div class="detail-row"><span class="k">Аллергия:</span> ${escapeHtml((medical.allergies || []).join(", ") || "—")}</div>
      <div class="detail-row"><span class="k">Созылмалы аурулар:</span> ${escapeHtml((medical.chronicConditions || []).join(", ") || "—")}</div>
      <div class="detail-row"><span class="k">Дәрі-дәрмек:</span> ${escapeHtml((medical.medications || []).join(", ") || "—")}</div>
    ` : `<div class="private-locked">Медициналық ақпарат қосылмаған.</div>`}

    <h3 style="margin-top:14px">Шұғыл байланыстар</h3>
    ${contacts.length ? contacts.map((c) => `
      <div class="detail-row">${escapeHtml(c.name)} (${escapeHtml(c.relationship || "—")}) — ${escapeHtml(c.phoneNumber)}</div>
    `).join("") : `<div class="private-locked">Байланыстар қосылмаған.</div>`}
  `;
  // Re-attach after innerHTML reset above didn't touch the note textarea (it lives outside this section).
}

async function updateStatus(incidentId, status) {
  await updateDoc(doc(db, "incidents", incidentId), { status, updatedAt: serverTimestamp() });
}

async function saveNote(incidentId) {
  const note = document.getElementById("noteInput").value;
  await setDoc(doc(db, "incidents", incidentId, "private", "dispatch"), { dispatcherNote: note }, { merge: true });
}

function formatCoords(incident) {
  if (!incident.latitude) return "—";
  return `${incident.latitude.toFixed(5)}, ${incident.longitude.toFixed(5)}`;
}

function formatTime(ts) {
  const millis = toMillis(ts);
  if (!millis) return "";
  return new Date(millis).toLocaleString("ru-RU");
}

function escapeHtml(str) {
  const div = document.createElement("div");
  div.textContent = str ?? "";
  return div.innerHTML;
}
