// SafeRide SA — operator compliance view
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

function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

function statusFor(dateStr) {
  if (!dateStr) return ['badge-blue', 'Unknown', 'info'];
  const d = new Date(dateStr);
  const today = new Date();
  const soon = new Date(); soon.setDate(soon.getDate() + 30);
  if (d < today) return ['badge-red', 'Expired', 'Renew immediately'];
  if (d < soon)  return ['badge-amber', 'Expiring soon', 'Renew within 30 days'];
  return ['badge-green', 'Valid', 'No action needed'];
}

async function load() {
  const res = await fetch(`${API}/operators/me`, { credentials: 'same-origin' });
  if (!res.ok) {
    document.getElementById('docTable').innerHTML =
      '<tr><td colspan="4" class="empty">No operator profile found</td></tr>';
    return;
  }
  const op = await res.json();
  document.getElementById('companyName').textContent = op.companyName || '—';

  const docs = [
    { name: 'PrDP',           expiry: op.prdpExpiry,         idPrefix: 'prdp' },
    { name: 'Roadworthy',     expiry: op.roadworthyExpiry,   idPrefix: 'rw' },
    { name: 'Registration',   expiry: op.registrationExpiry, idPrefix: 'reg' }
  ];

  // Stat cards
  for (const d of docs) {
    const [cls, label] = statusFor(d.expiry);
    const el = document.getElementById(d.idPrefix + 'Status');
    el.innerHTML = `<span class="badge ${cls}">${label}</span>`;
    document.getElementById(d.idPrefix + 'Expiry').textContent = 'Expires ' + (d.expiry || '—');
  }

  // Table
  const tbody = document.getElementById('docTable');
  tbody.innerHTML = '';
  for (const d of docs) {
    const [cls, label, action] = statusFor(d.expiry);
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(d.name)}</strong></td>
      <td>${escapeHtml(d.expiry || '—')}</td>
      <td><span class="badge ${cls}">${escapeHtml(label)}</span></td>
      <td style="color:var(--muted)">${escapeHtml(action)}</td>
    `;
    tbody.appendChild(tr);
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const user = requireLogin();
  if (!user) return;

  const chip = document.querySelector('.user-chip');
  const ini = user.fullName.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
  chip.innerHTML = `
    <div class="avatar">${ini}</div>
    <div>${user.fullName}<br><small>${user.role}</small></div>
  `;

  await load();

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
