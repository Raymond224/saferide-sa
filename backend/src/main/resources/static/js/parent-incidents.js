// SafeRide SA - parent report incident page
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

function escapeHtml(s) {
  if (!s) return '';

  return s.replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[c]));
}

function statusBadge(status) {
  const map = {
    open: ['badge-red', 'Open'],
    investigating: ['badge-amber', 'Investigating'],
    resolved: ['badge-green', 'Resolved']
  };

  const [cls, label] = map[status] || ['badge-blue', status || '—'];

  return `<span class="badge ${cls}">${label}</span>`;
}

async function loadTripOptions() {
  const select = document.getElementById('tripSelect');

  try {
    const res = await fetch(`${API}/trips/mine`, {
      credentials: 'same-origin'
    });

    if (!res.ok) return;

    const trips = await res.json();

    for (const t of trips) {
      const opt = document.createElement('option');

      opt.value = t.id;
      opt.textContent =
        `${t.routeName || 'Trip #' + t.id} — ${formatDate(t.departureTime)}`;

      select.appendChild(opt);
    }
  } catch (error) {
    // Trip selection is optional.
  }
}

async function loadMyReports() {
  const el = document.getElementById('myReports');

  try {
    const res = await fetch(`${API}/incidents/mine`, {
      credentials: 'same-origin'
    });

    if (res.status === 401) {
      window.location.href = '/index.html';
      return;
    }

    if (!res.ok) {
      el.innerHTML =
        '<p class="empty">Unable to load your incident reports</p>';
      return;
    }

    const incidents = await res.json();

    if (incidents.length === 0) {
      el.innerHTML =
        '<p class="empty">You have not reported any incidents</p>';
      return;
    }

    el.innerHTML = incidents.map(i => `
      <div class="activity">
        <div class="dot"></div>

        <div style="flex:1">
          <div style="display:flex;justify-content:space-between;align-items:baseline">
            <strong>${escapeHtml(i.type || '—')}</strong>
            ${statusBadge(i.statusString)}
          </div>

          <small style="color:var(--muted);display:block;margin-top:3px">
            ${formatDate(i.createdAt)}
            ${i.tripRouteName
              ? ' · ' + escapeHtml(i.tripRouteName)
              : ''}
          </small>

          <small style="display:block;margin-top:4px">
            ${escapeHtml(i.description || '')}
          </small>
        </div>
      </div>
    `).join('');
  } catch (error) {
    el.innerHTML =
      '<p class="empty">Unable to load your incident reports</p>';
  }
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

  await Promise.all([
    loadTripOptions(),
    loadMyReports()
  ]);

  document.getElementById('incidentForm').addEventListener('submit', async e => {
    e.preventDefault();

    const tripId = document.getElementById('tripSelect').value || null;
    const type = document.getElementById('typeSelect').value;
    const description =
      document.getElementById('description').value.trim();

    const msgBox = document.getElementById('formMsg');
    const btn = document.getElementById('submitBtn');

    if (!type) {
      alert('Please select a type');
      return;
    }

    if (!description) {
      alert('Please describe what happened');
      return;
    }

    btn.disabled = true;
    btn.textContent = 'Submitting...';
    msgBox.innerHTML = '';

    try {
      const res = await fetch(`${API}/incidents`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'same-origin',
        body: JSON.stringify({
          tripId: tripId ? Number(tripId) : null,
          type,
          description
        })
      });

      const data = await res.json();

      if (res.ok) {
        msgBox.innerHTML =
          '<div class="alert alert-info">✓ Report submitted. The school administrator has been notified.</div>';

        document.getElementById('incidentForm').reset();

        await loadMyReports();
      } else {
        msgBox.innerHTML =
          `<div class="alert alert-warning">Failed: ${escapeHtml(data.error || 'unknown error')}</div>`;
      }
    } catch (error) {
      msgBox.innerHTML =
        '<div class="alert alert-warning">Network error. Is the backend running?</div>';
    } finally {
      btn.disabled = false;
      btn.textContent = 'Submit Report';
    }
  });

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