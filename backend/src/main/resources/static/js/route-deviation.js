// SafeRide SA — admin route deviation page
const API = '/api';

let map = null;
let routePolyline = null;
let vehicleMarker = null;
let deviationMarker = null;

function currentUser() {
  try { return JSON.parse(localStorage.getItem('saferide.user') || 'null'); }
  catch { return null; }
}
function requireLogin() {
  const u = currentUser();
  if (!u) { window.location.href = '/index.html'; return null; }
  return u;
}

// Haversine distance in metres between two [lat,lng] points
function distanceMetres(a, b) {
  const R = 6371000;
  const toRad = d => d * Math.PI / 180;
  const dLat = toRad(b[0] - a[0]);
  const dLng = toRad(b[1] - a[1]);
  const lat1 = toRad(a[0]), lat2 = toRad(b[0]);
  const x = Math.sin(dLat/2)**2 +
            Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng/2)**2;
  return 2 * R * Math.asin(Math.sqrt(x));
}

// Minimum distance from a point to a polyline (list of [lat,lng])
function distanceToPolyline(point, line) {
  let min = Infinity;
  for (let i = 0; i < line.length - 1; i++) {
    const d = distanceToSegment(point, line[i], line[i+1]);
    if (d < min) min = d;
  }
  return min;
}

// Approximate: project point onto segment endpoints (good enough for demo)
function distanceToSegment(p, a, b) {
  return Math.min(distanceMetres(p, a), distanceMetres(p, b));
}

async function loadTrip(tripId) {
  const res = await fetch(`${API}/routes/trip/${tripId}`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  if (!res.ok) {
    document.getElementById('tripInfo').textContent = 'Trip not found';
    return;
  }
  const data = await res.json();

  // Info line
  document.getElementById('tripInfo').textContent =
    `${data.routeName} · ${data.operatorName} · ${data.vehicleRegistration}`;

  // Parse waypoints (JSON string)
  let waypoints = [];
  try { waypoints = JSON.parse(data.waypoints || '[]'); } catch (e) { waypoints = []; }

  const currentPos = (data.currentLat != null && data.currentLng != null)
    ? [Number(data.currentLat), Number(data.currentLng)]
    : null;

  // Distance from current position to the registered route
  let distanceOff = null;
  if (currentPos && waypoints.length >= 2) {
    distanceOff = distanceToPolyline(currentPos, waypoints);
  }

  // Show/hide alert banners based on trip status
  const isDeviated = data.status === 'deviated';
  document.getElementById('deviationAlert').style.display = isDeviated ? 'block' : 'none';
  document.getElementById('onRouteAlert').style.display = isDeviated ? 'none' : 'block';

  // Details panel
  document.getElementById('deviationDetails').innerHTML = `
    <p><strong>Status:</strong> ${data.status}</p>
    <p><strong>Departure:</strong> ${data.departureTime || '—'}</p>
    <p><strong>Current position:</strong> ${currentPos ? currentPos[0].toFixed(4) + ', ' + currentPos[1].toFixed(4) : '—'}</p>
    <p><strong>Distance from route:</strong> ${distanceOff != null ? Math.round(distanceOff) + ' m' : '—'}</p>
    <p><strong>Registered waypoints:</strong> ${waypoints.length}</p>
  `;

  // Learners list
  const learnersEl = document.getElementById('learnersList');
  if (!data.learners || data.learners.length === 0) {
    learnersEl.innerHTML = '<p class="empty">No learners assigned</p>';
  } else {
    learnersEl.innerHTML = data.learners.map(l => `
      <div class="activity">
        <div class="dot"></div>
        <div>
          <strong>${l.full_name}</strong>
          <small>${l.grade || ''} · ${l.picked_up ? '✅ Picked up' : '⏳ Not yet'} · ${l.dropped_off ? '🏠 Dropped off' : ''}</small>
        </div>
      </div>
    `).join('');
  }

  // Draw on the map
  drawMap(waypoints, currentPos, isDeviated);
}

function drawMap(waypoints, currentPos, isDeviated) {
  if (!map) {
    map = L.map('map').setView([-28.74, 24.77], 13);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(map);
  } else {
    // Clear old layers
    if (routePolyline) map.removeLayer(routePolyline);
    if (vehicleMarker) map.removeLayer(vehicleMarker);
    if (deviationMarker) map.removeLayer(deviationMarker);
  }

  // Registered route (blue dashed)
  if (waypoints.length >= 2) {
    routePolyline = L.polyline(waypoints, {
      color: '#1F7A8C',
      weight: 5,
      dashArray: '10,6',
      opacity: 0.8
    }).addTo(map);
    map.fitBounds(routePolyline.getBounds(), { padding: [40, 40] });
  }

  // Vehicle current position (red if deviated, green otherwise)
  if (currentPos) {
    vehicleMarker = L.circleMarker(currentPos, {
      radius: 10,
      color: isDeviated ? '#C53030' : '#2E7D32',
      fillColor: isDeviated ? '#C53030' : '#2E7D32',
      fillOpacity: 0.9,
      weight: 3
    }).addTo(map)
      .bindPopup(`Vehicle position (${isDeviated ? 'DEVIATED' : 'on route'})`);

    // If deviated, draw a dashed line to the nearest route point
    if (isDeviated && waypoints.length > 0) {
      let nearest = waypoints[0], minD = Infinity;
      for (const wp of waypoints) {
        const d = distanceMetres(currentPos, wp);
        if (d < minD) { minD = d; nearest = wp; }
      }
      deviationMarker = L.polyline([currentPos, nearest], {
        color: '#C53030',
        weight: 3,
        dashArray: '4,6'
      }).addTo(map);
    }
  }

  // Force Leaflet to recalculate size (fix for delayed layout)
  setTimeout(() => map.invalidateSize(), 100);
}

async function loadTripList() {
  const res = await fetch(`${API}/trips`, { credentials: 'same-origin' });
  if (!res.ok) return;
  const trips = await res.json();
  const sel = document.getElementById('tripSelect');
  sel.innerHTML = '';
  for (const t of trips) {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = `#${t.id} · ${t.routeName || 'Unnamed'} (${t.statusString})`;
    sel.appendChild(opt);
  }
  // Default to the deviated trip if there is one
  const deviated = trips.find(t => t.statusString === 'deviated');
  sel.value = deviated ? deviated.id : (trips[0] && trips[0].id);
  return sel.value;
}

document.addEventListener('DOMContentLoaded', async () => {
  const user = requireLogin();
  if (!user) return;

  const chip = document.querySelector('.user-chip');
  const initials = user.fullName.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  chip.innerHTML = `
    <div class="avatar">${initials}</div>
    <div>${user.fullName}<br><small>${user.role}</small></div>
  `;

  // Logout
  const logout = document.querySelector('.logout-link');
  logout.addEventListener('click', async (e) => {
    e.preventDefault();
    await fetch(`${API}/auth/logout`, { method: 'POST', credentials: 'same-origin' });
    localStorage.removeItem('saferide.user');
    window.location.href = '/index.html';
  });

  // Load trip list and default trip
  const initialTripId = await loadTripList();
  if (initialTripId) await loadTrip(initialTripId);

  // Change trip on select
  document.getElementById('tripSelect').addEventListener('change', (e) => {
    loadTrip(e.target.value);
  });

  // Notify parents
  document.getElementById('notifyBtn').addEventListener('click', async () => {
    const tripId = document.getElementById('tripSelect').value;
    if (!tripId) return;
    if (!confirm(`Send an alert to all parents of learners on trip #${tripId}?`)) return;

    const btn = document.getElementById('notifyBtn');
    btn.disabled = true;
    btn.textContent = 'Sending…';

    const res = await fetch(`${API}/routes/trip/${tripId}/notify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ message: 'Alert: possible route deviation on the school transport. Please check the app.' })
    });
    const data = await res.json();
    btn.disabled = false;
    btn.textContent = 'Notify All Parents';

    if (res.ok) {
      alert(`✓ Alert sent to ${data.sent} parent(s).`);
    } else {
      alert('Failed: ' + (data.error || 'unknown error'));
    }
  });
});
