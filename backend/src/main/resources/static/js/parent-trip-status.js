// SafeRide SA - parent trip status (active trips)
const API = '/api';
let expandedTripId = null;

function currentUser() {
  try {
    return JSON.parse(localStorage.getItem('saferide.user') || 'null');
  } catch {
    return null;
  }
}

function requireLogin() {
  const u = currentUser();
  if (!u) {
    window.location.href = '/index.html';
    return null;
  }
  return u;
}

function initials(name) {
  return (name || '')
    .split(' ')
    .map(w => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

function formatDate(iso) {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

function formatTime(iso) {
  if (!iso) return '—';
  return iso.slice(11, 16);
}

function tripBadge(status) {
  const map = {
    'on-route': ['badge-green', 'On Route'],
    'delayed': ['badge-red', 'Delayed'],
    'deviated': ['badge-red', 'Deviated'],
    'scheduled': ['badge-blue', 'Scheduled']
  };

  const [cls, label] = map[status] || ['badge-blue', status || '—'];
  return `<span class="badge ${cls}">${label}</span>`;
}

async function loadTrips() {
  const tbody = document.getElementById('tripsBody');

  try {
    const res = await fetch(`${API}/trips/mine?status=active`, {
      credentials: 'same-origin'
    });

    if (res.status === 401) {
      window.location.href = '/index.html';
      return;
    }

    if (!res.ok) {
      tbody.innerHTML =
        '<tr><td colspan="6" class="empty">Unable to load active trips</td></tr>';
      return;
    }

    const trips = await res.json();

    if (trips.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="6" class="empty">No active trips right now</td></tr>';
      return;
    }

    tbody.innerHTML = '';

    for (const t of trips) {
      const tr = document.createElement('tr');

      tr.innerHTML = `
        <td>${t.routeName || '—'}</td>
        <td>${t.operatorName || '—'}</td>
        <td>${t.vehicleRegistration || '—'}</td>
        <td>${formatTime(t.departureTime)}</td>
        <td>${tripBadge(t.statusString)}</td>
        <td>
          <button class="btn btn-outline btn-sm" data-trip-id="${t.id}">
            Details
          </button>
        </td>
      `;

      tbody.appendChild(tr);

      tr.querySelector('button').addEventListener('click', () => {
        toggleDetail(t.id, tr);
      });
    }
  } catch (error) {
    tbody.innerHTML =
      '<tr><td colspan="6" class="empty">Unable to load active trips</td></tr>';
  }
}

async function toggleDetail(tripId, rowEl) {
  const existing = document.getElementById(`detail-${tripId}`);

  if (existing) {
    existing.remove();
    expandedTripId = null;
    return;
  }

  document.querySelectorAll('[id^="detail-"]').forEach(e => e.remove());
  expandedTripId = tripId;

  const res = await fetch(`${API}/learners/mine?tripId=${tripId}`, {
    credentials: 'same-origin'
  });

  const learners = res.ok ? await res.json() : [];

  const detailRow = document.createElement('tr');
  detailRow.id = `detail-${tripId}`;

  detailRow.innerHTML = `
    <td colspan="6" style="background:var(--bg)">
      <strong>Your children on this trip</strong>

      <div style="margin-top:8px">
        ${
          learners.map(l => `
            <div style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--border)">
              <span>${l.fullName}</span>

              <span>
                ${
                  l.droppedOff
                    ? '<span class="badge badge-blue">Dropped Off</span>'
                    : l.pickedUp
                      ? '<span class="badge badge-green">Picked Up' +
                        (l.pickedUpAt
                          ? ' at ' + formatTime(l.pickedUpAt)
                          : '') +
                        '</span>'
                      : '<span class="badge badge-blue">Waiting for pickup</span>'
                }
              </span>
            </div>
          `).join('') ||
          '<p class="empty">No children found for this trip</p>'
        }
      </div>
    </td>
  `;

  rowEl.after(detailRow);
}

document.addEventListener('DOMContentLoaded', async () => {
  const user = requireLogin();

  if (!user) return;

  const chip = document.querySelector('.user-chip');

  chip.innerHTML = `
    <div class="avatar">${initials(user.fullName)}</div>
    <div>
      ${user.fullName}<br>
      <small>${user.role}</small>
    </div>
  `;

  await loadTrips();

  document.getElementById('refreshBtn').addEventListener('click', loadTrips);

  setInterval(loadTrips, 30000);

  document.querySelector('.logout-link').addEventListener('click', async e => {
    e.preventDefault();

    await fetch(`${API}/auth/logout`, {
      method: 'POST',
      credentials: 'same-origin'
    });

    localStorage.removeItem('saferide.user');
    window.location.href = '/index.html';
  });
});
