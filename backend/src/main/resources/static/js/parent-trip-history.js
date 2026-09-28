// SafeRide SA - parent trip history (completed trips)
const API = '/api';

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

async function loadHistory() {
  const tbody = document.getElementById('historyBody');

  try {
    const res = await fetch(`${API}/trips/mine?status=completed`, {
      credentials: 'same-origin'
    });

    if (res.status === 401) {
      window.location.href = '/index.html';
      return;
    }

    if (!res.ok) {
      tbody.innerHTML =
        '<tr><td colspan="5" class="empty">Unable to load trip history</td></tr>';
      return;
    }

    const trips = await res.json();

    if (trips.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="5" class="empty">No completed trips yet</td></tr>';
      return;
    }

    tbody.innerHTML = '';

    for (const t of trips) {
      const tr = document.createElement('tr');

      tr.innerHTML = `
        <td>${formatDate(t.departureTime)}</td>
        <td>${t.routeName || '—'}</td>
        <td>${t.operatorName || '—'}</td>
        <td>${t.vehicleRegistration || '—'}</td>
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
      '<tr><td colspan="5" class="empty">Unable to load trip history</td></tr>';
  }
}

async function toggleDetail(tripId, rowEl) {
  const existing = document.getElementById(`hdetail-${tripId}`);

  if (existing) {
    existing.remove();
    return;
  }

  document.querySelectorAll('[id^="hdetail-"]').forEach(e => e.remove());

  const res = await fetch(`${API}/learners/mine?tripId=${tripId}`, {
    credentials: 'same-origin'
  });

  const learners = res.ok ? await res.json() : [];

  const detailRow = document.createElement('tr');
  detailRow.id = `hdetail-${tripId}`;

  detailRow.innerHTML = `
    <td colspan="5" style="background:var(--bg)">
      <strong>Your children on this trip</strong>

      <div class="table-wrap" style="margin-top:8px">
        <table>
          <thead>
            <tr>
              <th>Child</th>
              <th>Picked Up</th>
              <th>Dropped Off</th>
            </tr>
          </thead>

          <tbody>
            ${
              learners.map(l => `
                <tr>
                  <td>${l.fullName}</td>
                  <td>${formatTime(l.pickedUpAt)}</td>
                  <td>${formatTime(l.droppedOffAt)}</td>
                </tr>
              `).join('') ||
              '<tr><td colspan="3" class="empty">No children found for this trip</td></tr>'
            }
          </tbody>
        </table>
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

  await loadHistory();

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
