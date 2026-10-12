// SafeRide SA — system-admin audit log viewer
const API = '/api';
let allLogs = [];

function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

function formatWhen(iso) {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

function actionBadge(action) {
  const map = {
    'login':             'badge-blue',
    'trip-created':      'badge-green',
    'incident-reported': 'badge-amber',
    'incident-updated':  'badge-amber',
    'user-created':      'badge-green',
    'notify-parent':     'badge-blue'
  };
  const cls = map[action] || 'badge-blue';
  return `<span class="badge ${cls}">${escapeHtml(action)}</span>`;
}

async function loadAudit() {
  const res = await fetch(`${API}/audit`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  if (!res.ok) {
    document.getElementById('auditTable').innerHTML =
      '<tr><td colspan="4" class="empty">Failed to load audit log</td></tr>';
    return;
  }
  allLogs = await res.json();
  renderAudit(allLogs);

  // Stats
  document.getElementById('statTotal').textContent = allLogs.length;
  const latest = allLogs[0];
  if (latest) {
    document.getElementById('statLatest').textContent =
      `${latest.action} (${formatWhen(latest.createdAt).slice(11)})`;
  }

  // Populate action filter dropdown
  const actions = [...new Set(allLogs.map(l => l.action))].sort();
  const sel = document.getElementById('auditActionFilter');
  for (const a of actions) {
    const opt = document.createElement('option');
    opt.value = a;
    opt.textContent = a;
    sel.appendChild(opt);
  }
}

function renderAudit(logs) {
  const tbody = document.getElementById('auditTable');
  tbody.innerHTML = '';

  if (logs.length === 0) {
    tbody.innerHTML = '<tr><td colspan="4" class="empty">No audit entries</td></tr>';
    return;
  }

  for (const log of logs) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${escapeHtml(formatWhen(log.createdAt))}</td>
      <td>${escapeHtml(log.userName || '—')}<br><small style="color:var(--muted)">${escapeHtml(log.userRole || '')}</small></td>
      <td>${actionBadge(log.action)}</td>
      <td>${escapeHtml(log.details || '')}</td>
    `;
    tbody.appendChild(tr);
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  await loadAudit();

  // Search
  document.getElementById('auditSearch').addEventListener('input', filter);
  document.getElementById('auditActionFilter').addEventListener('change', filter);

  function filter() {
    const q = document.getElementById('auditSearch').value.toLowerCase();
    const action = document.getElementById('auditActionFilter').value;
    const filtered = allLogs.filter(l => {
      const matchQ = !q ||
        (l.details || '').toLowerCase().includes(q) ||
        (l.userName || '').toLowerCase().includes(q) ||
        (l.action || '').toLowerCase().includes(q);
      const matchAction = !action || l.action === action;
      return matchQ && matchAction;
    });
    renderAudit(filtered);
  }

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
