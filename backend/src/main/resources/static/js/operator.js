// SafeRide SA — operator dashboard
const API = '/api';

function currentUser() {
  try { return JSON.parse(localStorage.getItem('saferide.user') || 'null'); }
  catch { return null; }
}
function requireLogin() {
  const u = currentUser();
  if (!u) { window.location.href = '/index.html'; return null; }
  return u;
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

function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

async function loadTrips() {
  const res = await fetch(`${API}/trips/mine`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  const trips = await res.json();

  const active = trips.filter(t => ['on-route', 'delayed', 'deviated'].includes(t.statusString));

  document.getElementById('statActiveTrips').textContent = active.length;
  const delayed = active.filter(t => t.statusString === 'delayed').length;
  const deviated = active.filter(t => t.statusString === 'deviated').length;
  document.getElementById('statActiveTripsNote').textContent =
    `${delayed} delayed · ${deviated} deviated`;

  document.getElementById('statLearners').textContent = active.length * 4; // approx, real count via register

  const tbody = document.getElementById('tripsBody');
  tbody.innerHTML = '';
  if (active.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty">No active trips</td></tr>';
    return;
  }
  for (const t of active) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(t.routeName || '—')}</strong></td>
      <td>${escapeHtml(t.vehicleRegistration || '—')}</td>
      <td>${t.departureTime ? t.departureTime.slice(11, 16) : '—'}</td>
      <td>${tripBadge(t.statusString)}</td>
      <td><a href="trip-register.html?tripId=${t.id}" class="btn btn-outline btn-sm">Register</a></td>
    `;
    tbody.appendChild(tr);
  }
}

async function loadCompliance() {
  // Use /api/operators/me to get this operator's row
  const res = await fetch(`${API}/operators/me`, { credentials: 'same-origin' });
  if (!res.ok) {
    document.getElementById('complianceBox').innerHTML =
      '<p class="empty">No operator profile</p>';
    return;
  }
  const op = await res.json();
  document.getElementById('statCompany').textContent = op.companyName || '—';

  // Compute status per document
  const today = new Date();
  const soon = new Date();
  soon.setDate(soon.getDate() + 30);

  function status(dateStr) {
    if (!dateStr) return ['badge-blue', 'Unknown'];
    const d = new Date(dateStr);
    if (d < today) return ['badge-red', 'Expired'];
    if (d < soon) return ['badge-amber', 'Expiring soon'];
    return ['badge-green', 'Valid'];
  }

  const [prdpCls, prdpLbl] = status(op.prdpExpiry);
  const [rwCls, rwLbl] = status(op.roadworthyExpiry);
  const [regCls, regLbl] = status(op.registrationExpiry);

  const alerts = [prdpLbl, rwLbl, regLbl].filter(s => s !== 'Valid').length;
  document.getElementById('statCompliance').textContent = alerts;
  document.getElementById('statComplianceNote').textContent =
    alerts === 0 ? 'All good' : alerts + ' document(s) need attention';

  document.getElementById('complianceBox').innerHTML = `
    <div class="activity">
      <div class="dot"></div>
      <div><strong>PrDP</strong> <small>Expires ${op.prdpExpiry || '—'}</small></div>
      <div style="margin-left:auto">${'<span class="badge ' + prdpCls + '">' + prdpLbl + '</span>'}</div>
    </div>
    <div class="activity">
      <div class="dot"></div>
      <div><strong>Roadworthy</strong> <small>Expires ${op.roadworthyExpiry || '—'}</small></div>
      <div style="margin-left:auto">${'<span class="badge ' + rwCls + '">' + rwLbl + '</span>'}</div>
    </div>
    <div class="activity">
      <div class="dot"></div>
      <div><strong>Registration</strong> <small>Expires ${op.registrationExpiry || '—'}</small></div>
      <div style="margin-left:auto">${'<span class="badge ' + regCls + '">' + regLbl + '</span>'}</div>
    </div>
  `;
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
  document.getElementById('greetingName').textContent = user.fullName.split(' ')[0];

  await loadTrips();
  await loadCompliance();

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
