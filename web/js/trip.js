// No Firebase SDK here on purpose — this page never talks to Firestore
// directly. It only calls the getPublicTripStatus Cloud Function (see
// functions/index.js), which returns a deliberately narrow field subset for
// exactly one trip id. The `trips` collection's Firestore rule locks reads
// to the trip's own owner, so this function (Admin SDK, bypasses rules) is
// the only way a link recipient sees live progress without signing in.

const STATUS_LABELS = {
  ACTIVE: "Жолда", ARRIVED: "Сәтті келді", ALERTED: "Мерзімі өтті", CANCELLED: "Болдырылмады",
};

const POLL_INTERVAL_MS = 5000;
const tripId = new URLSearchParams(window.location.search).get("id");

const emptyEl = document.getElementById("trackEmpty");
const statusBarEl = document.getElementById("trackStatusBar");
const badgeEl = document.getElementById("trackBadge");
const destinationEl = document.getElementById("trackDestination");
const deadlineEl = document.getElementById("trackDeadline");
const mapEl = document.getElementById("trackMap");

let map = null;
let marker = null;

if (!tripId) {
  emptyEl.textContent = "Сілтеме жарамсыз — сапар идентификаторы жоқ.";
} else {
  poll();
  const interval = setInterval(async () => {
    const stillLive = await poll();
    if (!stillLive) clearInterval(interval);
  }, POLL_INTERVAL_MS);
}

async function poll() {
  try {
    const res = await fetch(`/api/trip-status?id=${encodeURIComponent(tripId)}`);
    if (res.status === 404) {
      emptyEl.textContent = "Сапар табылмады.";
      return false;
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    render(data);
    return data.statusName === "ACTIVE";
  } catch (err) {
    if (!map) emptyEl.textContent = "Деректерді жүктеу мүмкін болмады. Қайта көру...";
    return true;
  }
}

function render(data) {
  emptyEl.hidden = true;
  statusBarEl.hidden = false;
  mapEl.hidden = false;

  badgeEl.textContent = STATUS_LABELS[data.statusName] ?? data.statusName ?? "—";
  badgeEl.className = `badge ${data.statusName ?? ""}`;
  destinationEl.textContent = data.destination ?? "";
  deadlineEl.textContent = data.expectedArrivalAtEpochMs
    ? `Келу уақыты: ${new Date(data.expectedArrivalAtEpochMs).toLocaleTimeString("kk-KZ", { hour: "2-digit", minute: "2-digit" })}`
    : "";

  if (!data.latitude || !data.longitude) return;

  if (!map) {
    map = L.map("trackMap").setView([data.latitude, data.longitude], 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; OpenStreetMap contributors",
      maxZoom: 19,
    }).addTo(map);
    marker = L.circleMarker([data.latitude, data.longitude], {
      radius: 10, fillColor: "#0b4da6", color: "#0b4da6", weight: 2, fillOpacity: 0.85,
    }).addTo(map);
  } else {
    marker.setLatLng([data.latitude, data.longitude]);
    map.panTo([data.latitude, data.longitude]);
  }
}
