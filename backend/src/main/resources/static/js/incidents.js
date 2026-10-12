// SafeRide SA — admin incidents page
const API = '/api';
let allIncidents = [];

function currentUser() {
  try { return JSON.parse(localStorage.getItem('saferide.user') || 'null'); }
  catch { return null; }
}
function requireLogin() {
  const u = currentUser();
  if (!u) { window.location.href = '/index.html'; return null; }
  return u;
}

function statusBadge(status) {
  const map = {
    'open':          ['badge-red',   'Open'],
    'investigating': ['badge-amber', 'Investigating'],
    'resolved':      ['badge-green', 'Resolved']
  };
  const [cls, label] = map[status] || ['badge-blue', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

function formatDate(iso) {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

async function loadIncidents(filterStatus) {
  const url = filterStatus ? `${API}/incidents?status=${filterStatus}` : `${API}/incidents`;
  const res = await fetch(url, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  allIncidents = await res.json();

  // Update summary counts (from full list, not filtered view — refresh separately)
  if (!filterStatus) {
    const open = allIncidents.filter(i => i.statusString === 'open').length;
    const inv  = allIncidents.filter(i => i.statusString === 'investigating').length;
    const res  = allIncidents.filter(i => i.statusString === 'resolved').length;
    document.getElementById('statOpen').textContent = open;
    document.getElementById('statInvestigating').textContent = inv;
    document.getElementById('statResolved').textContent = res;
    document.getElementById('statTotal').textContent = allIncidents.length;
  }

  renderTable(allIncidents);
}

function renderTable(incidents) {
  const tbody = document.getElementById('incidentsBody');
  tbody.innerHTML = '';

  if (incidents.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty">No incidents</td></tr>';
    return;
  }

  for (const inc of incidents) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${formatDate(inc.createdAt)}</td>
      <td>${inc.reporterName || '—'} <small>(${inc.reporterRole || ''})</small></td>
      <td>${inc.type || '—'}</td>
      <td>${inc.tripRouteName || '—'}</td>
      <td>${statusBadge(inc.statusString)}</td>
      <td>
        <select class="select" data-incident-id="${inc.id}" style="padding:6px 8px;font-size:12px">
          <option value="open"          ${inc.statusString === 'open' ? 'selected' : ''}>Open</option>
          <option value="investigating" ${inc.statusString === 'investigating' ? 'selected' : ''}>Investigating</option>
          <option value="resolved"      ${inc.statusString === 'resolved' ? 'selected' : ''}>Resolved</option>
        </select>
      </td>
    `;
    tbody.appendChild(tr);

    // Wire the change handler
    const sel = tr.querySelector('select');
    sel.addEventListener('change', async (e) => {
      const newStatus = e.target.value;
      const incidentId = e.target.dataset.incidentId;
      await updateStatus(incidentId, newStatus, e.target);
    });
  }
}

async function updateStatus(incidentId, newStatus, selectEl) {
  selectEl.disabled = true;
  try {
    const res = await fetch(`${API}/incidents/${incidentId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ status: newStatus })
    });
    const data = await res.json();
    if (!res.ok) {
      alert('Update failed: ' + (data.error || 'unknown'));
      selectEl.value = selectEl.dataset.original || 'open';
    } else {
      // Reload to refresh summary counts + row colours
      await loadIncidents(document.getElementById('statusFilter').value || null);
    }
  } catch (err) {
    alert('Network error');
  } finally {
    selectEl.disabled = false;
  }
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

  await loadIncidents(null);

  // Filter change
  document.getElementById('statusFilter').addEventListener('change', (e) => {
    const v = e.target.value;
    loadIncidents(v || null);
  });

  // Logout
  const logout = document.querySelector('.logout-link');
  logout.addEventListener('click', async (e) => {
    e.preventDefault();
    await fetch(`${API}/auth/logout`, { method: 'POST', credentials: 'same-origin' });
    localStorage.removeItem('saferide.user');
    window.location.href = '/index.html';
  });
});
