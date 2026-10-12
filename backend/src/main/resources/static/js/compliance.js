// SafeRide SA — admin compliance page
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

// Map status → badge class + label
function statusBadge(status) {
  const map = {
    'valid':        ['badge-green', 'Valid'],
    'expiring':     ['badge-amber', 'Expiring soon'],
    'expired':      ['badge-red',   'Expired'],
    'unknown':      ['badge-blue',  'Unknown']
  };
  const [cls, label] = map[status] || ['badge-blue', status];
  return `<span class="badge ${cls}">${label}</span>`;
}

function overallBadge(overall) {
  const map = {
    'compliant':     ['badge-green', 'Compliant'],
    'expiring-soon': ['badge-amber', 'Expiring soon'],
    'non-compliant': ['badge-red',   'Non-compliant']
  };
  const [cls, label] = map[overall] || ['badge-blue', overall];
  return `<span class="badge ${cls}">${label}</span>`;
}

async function loadSummary() {
  const res = await fetch(`${API}/compliance/summary`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  const s = await res.json();
  document.getElementById('statOperators').textContent = s.totalOperators;
  document.getElementById('statExpired').textContent = s.nonCompliant;
  document.getElementById('statExpiring').textContent = s.expiringSoon;
  document.getElementById('statCompliant').textContent = s.compliant;
}

async function loadTable() {
  const res = await fetch(`${API}/compliance`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  const operators = await res.json();

  const tbody = document.getElementById('complianceBody');
  tbody.innerHTML = '';
  if (operators.length === 0) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty">No operators</td></tr>';
    return;
  }
  for (const op of operators) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${op.companyName}</strong></td>
      <td>${statusBadge(op.prdpStatus)}<br><small>${op.prdpExpiry || '—'}</small></td>
      <td>${statusBadge(op.roadworthyStatus)}<br><small>${op.roadworthyExpiry || '—'}</small></td>
      <td>${statusBadge(op.registrationStatus)}<br><small>${op.registrationExpiry || '—'}</small></td>
      <td>${overallBadge(op.overall)}</td>
    `;
    tbody.appendChild(tr);
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

  await loadSummary();
  await loadTable();

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
