// No Firebase SDK here on purpose — this page never talks to Firestore
// directly. It only calls the getPublicIncidentStatus Cloud Function (see
// functions/index.js), which returns a deliberately narrow field subset for
// exactly one incident id. A public page with direct Firestore read access
// could otherwise be used to scrape every reporter's location, not just the
// one this link is for.

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

const POLL_INTERVAL_MS = 5000;
const incidentId = new URLSearchParams(window.location.search).get("id");

const emptyEl = document.getElementById("trackEmpty");
const statusBarEl = document.getElementById("trackStatusBar");
const badgeEl = document.getElementById("trackBadge");
const labelEl = document.getElementById("trackLabel");
const mapEl = document.getElementById("trackMap");

let map = null;
let marker = null;

if (!incidentId) {
  emptyEl.textContent = "Сілтеме жарамсыз — оқиға идентификаторы жоқ.";
} else {
  poll();
  setInterval(poll, POLL_INTERVAL_MS);
}

async function poll() {
  try {
    const res = await fetch(`/api/incident-status?id=${encodeURIComponent(incidentId)}`);
    if (res.status === 404) {
      emptyEl.textContent = "Оқиға табылмады.";
      return;
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    render(data);
  } catch (err) {
    // Keep the last good render on a transient network error; only show an
    // error state if we've never successfully loaded anything yet.
    if (!map) emptyEl.textContent = "Деректерді жүктеу мүмкін болмады. Қайта көру...";
  }
}

function render(data) {
  emptyEl.hidden = true;
  statusBarEl.hidden = false;
  mapEl.hidden = false;

  badgeEl.textContent = STATUS_LABELS[data.status] ?? data.status ?? "—";
  badgeEl.className = `badge ${data.status ?? ""}`;
  labelEl.textContent = TYPE_LABELS[data.type] ?? data.type ?? "";
  if (data.isSosTriggered) labelEl.textContent += " · SOS";

  if (!data.latitude || !data.longitude) return;

  if (!map) {
    map = L.map("trackMap").setView([data.latitude, data.longitude], 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap contributors",
      maxZoom: 19,
    }).addTo(map);
    marker = L.circleMarker([data.latitude, data.longitude], {
      radius: 10, fillColor: "#e0301f", color: "#e0301f", weight: 2, fillOpacity: 0.85,
    }).addTo(map);
  } else {
    marker.setLatLng([data.latitude, data.longitude]);
    map.panTo([data.latitude, data.longitude]);
  }
}
