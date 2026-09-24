// SafeRide SA — admin operators page
const API = '/api';
let allOperators = [];
let complianceById = {};

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
    'valid':    ['badge-green', 'Valid'],
    'expiring': ['badge-amber', 'Expiring soon'],
    'expired':  ['badge-red',   'Expired'],
    'unknown':  ['badge-blue',  'Unknown']
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

function escapeHtml(s) {
  if (!s) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

async function loadCompliance() {
  const res = await fetch(`${API}/compliance`, { credentials: 'same-origin' });
  if (!res.ok) return;
  const list = await res.json();
  complianceById = {};
  for (const c of list) complianceById[c.id] = c;

  // Summary counts
  let compliant = 0, expiring = 0, expired = 0;
  for (const c of list) {
    if (c.overall === 'compliant') compliant++;
    else if (c.overall === 'expiring-soon') expiring++;
    else expired++;
  }
  document.getElementById('statTotal').textContent = list.length;
  document.getElementById('statCompliant').textContent = compliant;
  document.getElementById('statExpiring').textContent = expiring;
  document.getElementById('statExpired').textContent = expired;
}

async function loadOperators() {
  const res = await fetch(`${API}/operators`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  allOperators = await res.json();
  renderTable(allOperators);
}

function renderTable(operators) {
  const tbody = document.getElementById('operatorsBody');
  tbody.innerHTML = '';

  if (!operators || operators.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty">No operators</td></tr>';
    return;
  }

  for (const op of operators) {
    const comp = complianceById[op.id] || {};
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(op.companyName)}</strong></td>
      <td>${escapeHtml(op.contactName) || '—'}</td>
      <td>${escapeHtml(op.contactEmail) || '—'}</td>
      <td>${statusBadge(comp.prdpStatus)}</td>
      <td>${statusBadge(comp.roadworthyStatus)}</td>
      <td>${overallBadge(comp.overall)}</td>
      <td><a href="operator-detail.html?id=${op.id}" class="btn btn-outline btn-sm">View</a></td>
    `;
    tbody.appendChild(tr);
  }
}

async function submitOperator() {
  const form = document.getElementById('addOperatorForm');
  const formData = new FormData(form);
  const payload = {};
  for (const [k, v] of formData.entries()) {
    payload[k] = v.trim() === '' ? null : v.trim();
  }

  const required = ['companyName', 'contactName', 'username', 'password'];
  for (const k of required) {
    if (!payload[k]) {
      showStatus('error', `Please fill in: ${k}`);
      return;
    }
  }
  if (payload.password.length < 6) {
    showStatus('error', 'Password must be at least 6 characters');
    return;
  }

  const btn = document.getElementById('saveOperator');
  btn.disabled = true;
  btn.textContent = 'Saving…';

  try {
    const res = await fetch(`${API}/operators`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (!res.ok) {
      showStatus('error', data.error || 'Failed to save');
      btn.disabled = false;
      btn.textContent = 'Save Operator';
      return;
    }

    showStatus('success', `✓ Operator created (id ${data.id}). Login: ${payload.username} / ${payload.password}`);
    form.reset();
    btn.disabled = false;
    btn.textContent = 'Save Operator';

    await loadOperators();
    await loadCompliance();
  } catch (err) {
    showStatus('error', 'Network error: ' + err.message);
    btn.disabled = false;
    btn.textContent = 'Save Operator';
  }
}

function showStatus(type, message) {
  const box = document.getElementById('formStatus');
  if (type === 'success') {
    box.innerHTML = `<div class="alert alert-info" style="background:#DCFCE7;color:#166534">${escapeHtml(message)}</div>`;
  } else {
    box.innerHTML = `<div class="alert alert-warning" style="background:#FEE2E2;color:#991B1B">${escapeHtml(message)}</div>`;
  }
  setTimeout(() => { box.innerHTML = ''; }, 8000);
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

  await loadCompliance();
  await loadOperators();

  // Save button
  document.getElementById('saveOperator').addEventListener('click', submitOperator);

  // Reset button
  document.getElementById('resetForm').addEventListener('click', () => {
    document.getElementById('addOperatorForm').reset();
    document.getElementById('formStatus').innerHTML = '';
  });

  // Search
  document.getElementById('searchInput').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    const filtered = allOperators.filter(o =>
      (o.companyName || '').toLowerCase().includes(q) ||
      (o.contactName || '').toLowerCase().includes(q) ||
      (o.contactEmail || '').toLowerCase().includes(q)
    );
    renderTable(filtered);
  });

  // Logout
  document.querySelector('.logout-link').addEventListener('click', async (e) => {
    e.preventDefault();
    await fetch(`${API}/auth/logout`, { method: 'POST', credentials: 'same-origin' });
    localStorage.removeItem('saferide.user');
    window.location.href = '/index.html';
  });
});
