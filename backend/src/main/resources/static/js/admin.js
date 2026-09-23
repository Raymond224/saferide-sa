// SafeRide SA — admin dashboard
const API = '/api';

function currentUser() {
  try { return JSON.parse(localStorage.getItem('saferide.user') || 'null'); }
  catch { return null; }
}

function requireLogin() {
  const user = currentUser();
  if (!user) { window.location.href = '/index.html'; return null; }
  return user;
}

function tripBadge(status) {
  const map = {
    'on-route':  ['badge-green', 'On Route'],
    'delayed':   ['badge-amber', 'Delayed'],
    'deviated':  ['badge-red',   'Deviated'],
    'completed': ['badge-blue',  'Completed'],
    'scheduled': ['badge-blue',  'Scheduled']
  };
  const [cls, label] = map[status] || ['badge-blue', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

async function loadTrips() {
  const res = await fetch(`${API}/trips?status=active`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  const trips = await res.json();

  document.getElementById('statActiveTrips').textContent = trips.length;
  const delayed = trips.filter(t => t.statusString === 'delayed').length;
  const deviated = trips.filter(t => t.statusString === 'deviated').length;
  document.getElementById('statActiveTripsNote').textContent =
    `${delayed} delayed · ${deviated} deviated`;

  const tbody = document.getElementById('liveTripsBody');
  tbody.innerHTML = '';
  if (trips.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" class="empty">No active trips</td></tr>';
    return;
  }
  for (const t of trips) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${t.operatorName || '—'}</td>
      <td>${t.routeName || '—'}</td>
      <td>${t.vehicleRegistration || '—'}</td>
      <td>${tripBadge(t.statusString)}</td>
    `;
    tbody.appendChild(tr);
  }
}

async function loadCounts() {
  // Open incidents — real count
  try {
    const res = await fetch(`${API}/incidents?status=open`, { credentials: 'same-origin' });
    if (res.ok) {
      const incidents = await res.json();
      document.getElementById('statIncidents').textContent = incidents.length;
    }
  } catch (e) { /* ignore */ }

  // Placeholders — will be replaced when compliance/messages endpoints exist
  document.getElementById('statCompliance').textContent = 2;
  document.getElementById('statComplianceNote').textContent = '1 expired · 1 expiring soon';
  document.getElementById('statMessages').textContent = 5;
}

document.addEventListener('DOMContentLoaded', async () => {
  const user = requireLogin();
  if (!user) return;

  // User chip
  const chip = document.querySelector('.user-chip');
  const initials = user.fullName.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  chip.innerHTML = `
    <div class="avatar">${initials}</div>
    <div>${user.fullName}<br><small>${user.role}</small></div>
  `;

  // Greeting
  const firstName = user.fullName.split(' ')[0];
  document.getElementById('greetingName').textContent = firstName;

  await loadTrips();
  await loadCounts();

  // Logout
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
