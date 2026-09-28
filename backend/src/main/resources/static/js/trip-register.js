// SafeRide SA — operator learner register
const API = '/api';
let currentTripId = null;
let allTrips = [];

function currentUser() {
  try { return JSON.parse(localStorage.getItem('saferide.user') || 'null'); }
  catch { return null; }
}
function requireLogin() {
  const u = currentUser();
  if (!u) { window.location.href = '/index.html'; return null; }
  return u;
}

function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

function initials(name) {
  if (!name) return '?';
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
}

async function loadTrips() {
  const res = await fetch(`${API}/trips/mine`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return []; }
  return res.json();
}

function populatePicker(trips) {
  const sel = document.getElementById('tripPicker');
  sel.innerHTML = '';
  if (trips.length === 0) {
    const opt = document.createElement('option');
    opt.textContent = 'No trips assigned';
    opt.value = '';
    sel.appendChild(opt);
    return;
  }
  for (const t of trips) {
    const opt = document.createElement('option');
    opt.value = t.id;
    opt.textContent = `#${t.id} · ${t.routeName || 'Unnamed'} (${t.statusString})`;
    sel.appendChild(opt);
  }
}

async function loadTrip(tripId) {
  currentTripId = tripId;

  // Get trip details
  const tripRes = await fetch(`${API}/trips/${tripId}`, { credentials: 'same-origin' });
  if (!tripRes.ok) {
    document.getElementById('tripTitle').textContent = 'Trip not found';
    return;
  }
  const trip = await tripRes.json();

  document.getElementById('tripTitle').textContent = trip.routeName || 'Trip';
  document.getElementById('tripSubtitle').textContent =
    `Vehicle ${trip.vehicleRegistration || '—'} · Status ${trip.statusString}`;

  // Get learners from route endpoint
  const learnersRes = await fetch(`${API}/routes/trip/${tripId}`, { credentials: 'same-origin' });
  if (!learnersRes.ok) {
    document.getElementById('learnersBox').innerHTML =
      '<p class="empty">Could not load learners</p>';
    return;
  }
  const data = await learnersRes.json();
  renderLearners(data.learners || []);
}

function renderLearners(learners) {
  const box = document.getElementById('learnersBox');

  if (learners.length === 0) {
    box.innerHTML = '<p class="empty">No learners assigned to this trip</p>';
    document.getElementById('countPickedUp').textContent = 0;
    document.getElementById('countDroppedOff').textContent = 0;
    document.getElementById('countTotal').textContent = 0;
    return;
  }

  box.innerHTML = learners.map(l => {
    const pickedUp = l.picked_up === 1 || l.picked_up === true;
    const droppedOff = l.dropped_off === 1 || l.dropped_off === true;
    const stateClass = droppedOff ? 'state-dropped'
                     : pickedUp ? 'state-picked'
                     : 'state-pending';

    const bg = droppedOff ? '#DBEAFE'
             : pickedUp ? '#DCFCE7'
             : '#F1F5F9';
    const border = droppedOff ? '#93C5FD'
                 : pickedUp ? '#86EFAC'
                 : '#E5E7EB';

    return `
      <div style="background:${bg};border:1.5px solid ${border};border-radius:12px;padding:16px 20px;margin-bottom:12px;display:flex;align-items:center;justify-content:space-between;gap:16px">
        <div style="display:flex;align-items:center;gap:14px">
          <div style="width:44px;height:44px;border-radius:50%;background:linear-gradient(135deg,#123B5D,#1F7A8C);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:14px">
            ${initials(l.full_name)}
          </div>
          <div>
            <div style="font-weight:700;font-size:15px">${escapeHtml(l.full_name)}</div>
            <div style="font-size:13px;color:var(--muted)">${escapeHtml(l.grade || '')}</div>
            <div style="font-size:12px;color:var(--muted);margin-top:2px">
              ${pickedUp ? '✓ Picked up' + (l.picked_up_at ? ' at ' + l.picked_up_at.slice(11, 16) : '') : '⏳ Not yet picked up'}
              ${droppedOff ? ' · 🏠 Dropped off' : ''}
            </div>
          </div>
        </div>
        <div style="display:flex;flex-direction:column;gap:8px;min-width:170px">
          ${pickedUp ? `
            <button class="btn btn-outline btn-sm" onclick="togglePickup(${l.learner_id}, false)">Undo pick-up</button>
          ` : `
            <button class="btn btn-primary btn-sm" onclick="togglePickup(${l.learner_id}, true)">Mark picked up</button>
          `}
          ${pickedUp && !droppedOff ? `
            <button class="btn btn-outline btn-sm" onclick="toggleDropoff(${l.learner_id})">Mark dropped off</button>
          ` : ''}
        </div>
      </div>
    `;
  }).join('');

  // Update counters
  const pickedUp = learners.filter(l => l.picked_up === 1 || l.picked_up === true).length;
  const droppedOff = learners.filter(l => l.dropped_off === 1 || l.dropped_off === true).length;
  document.getElementById('countPickedUp').textContent = pickedUp;
  document.getElementById('countDroppedOff').textContent = droppedOff;
  document.getElementById('countTotal').textContent = learners.length;
}

async function togglePickup(learnerId, value) {
  const res = await fetch(`${API}/trips/${currentTripId}/learners/${learnerId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify({ pickedUp: value })
  });
  if (res.ok) {
    await loadTrip(currentTripId);
  } else {
    alert('Failed to update');
  }
}

async function toggleDropoff(learnerId) {
  const res = await fetch(`${API}/trips/${currentTripId}/learners/${learnerId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin',
    body: JSON.stringify({ droppedOff: true })
  });
  if (res.ok) {
    await loadTrip(currentTripId);
  } else {
    alert('Failed to update');
  }
}

window.togglePickup = togglePickup;
window.toggleDropoff = toggleDropoff;

document.addEventListener('DOMContentLoaded', async () => {
  const user = requireLogin();
  if (!user) return;

  const chip = document.querySelector('.user-chip');
  const ini = user.fullName.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  chip.innerHTML = `
    <div class="avatar">${ini}</div>
    <div>${user.fullName}<br><small>${user.role}</small></div>
  `;

  allTrips = await loadTrips();
  populatePicker(allTrips);

  // Default: URL param, else first trip
  const urlParams = new URLSearchParams(window.location.search);
  const urlTrip = urlParams.get('tripId');
  const defaultTrip = urlTrip || (allTrips[0] ? allTrips[0].id : null);

  if (defaultTrip) {
    document.getElementById('tripPicker').value = defaultTrip;
    await loadTrip(defaultTrip);
  } else {
    document.getElementById('tripTitle').textContent = 'No trips assigned';
    document.getElementById('learnersBox').innerHTML =
      '<p class="empty">Create a trip first</p>';
  }

  document.getElementById('tripPicker').addEventListener('change', (e) => {
    if (e.target.value) loadTrip(e.target.value);
  });

  const logout = document.querySelector('.logout-link');
  if (logout) {
    logout.addEventListener('click', async (e) => {
      e.preventDefault();
      await fetch(`${API}/auth/logout`, { method: 'POST', credentials: 'same-origin' });
      localStorage.removeItem('saferide.user');
      window.location.href = '/index.html';
    });
  }
});
