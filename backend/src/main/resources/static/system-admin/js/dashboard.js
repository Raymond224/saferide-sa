// SafeRide SA — system-admin dashboard
const API = '/api';

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

function initials(name) {
  if (!name) return 'SA';
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
}

async function loadStats() {
  // Users
  try {
    const res = await fetch(`${API}/users`, { credentials: 'same-origin' });
    if (res.ok) {
      const users = await res.json();
      document.getElementById('statTotalUsers').textContent = users.length;
      document.getElementById('statActiveUsers').textContent =
        users.filter(u => u.active).length;
    }
  } catch (e) { /* ignore */ }

  // Schools
  try {
    const res = await fetch(`${API}/schools`, { credentials: 'same-origin' });
    if (res.ok) {
      const schools = await res.json();
      document.getElementById('statSchools').textContent = schools.length;
    }
  } catch (e) { /* ignore */ }

  // Audit count
  try {
    const res = await fetch(`${API}/audit`, { credentials: 'same-origin' });
    if (res.ok) {
      const logs = await res.json();
      document.getElementById('statAudit').textContent = logs.length;
    }
  } catch (e) { /* ignore */ }
}

async function loadRecentActivity() {
  const res = await fetch(`${API}/audit`, { credentials: 'same-origin' });
  if (!res.ok) {
    document.getElementById('recentActivity').innerHTML =
      '<p class="empty">Could not load activity</p>';
    return;
  }
  const logs = await res.json();
  const recent = logs.slice(0, 5);

  const el = document.getElementById('recentActivity');
  if (recent.length === 0) {
    el.innerHTML = '<p class="empty">No activity yet</p>';
    return;
  }

  el.innerHTML = recent.map(log => `
    <div class="activity">
      <div class="dot"></div>
      <div>
        <strong>${escapeHtml(log.action)}</strong>
        <small>${escapeHtml(formatWhen(log.createdAt))} · ${escapeHtml(log.userName || 'system')}</small>
        <small style="color:var(--muted);margin-top:3px;display:block">${escapeHtml(log.details || '')}</small>
      </div>
    </div>
  `).join('');
}

async function loadSystemAlerts() {
  const alerts = [];

  // Check compliance alerts count
  try {
    const res = await fetch(`${API}/compliance/summary`, { credentials: 'same-origin' });
    if (res.ok) {
      const s = await res.json();
      if (s.totalAlerts > 0) {
        alerts.push({
          level: 'warning',
          title: `${s.totalAlerts} compliance alerts`,
          text: `${s.nonCompliant} expired · ${s.expiringSoon} expiring soon`,
          bg: '#FFFBEB', color: '#92400E'
        });
      }
    }
  } catch (e) { /* ignore */ }

  // Check open incidents
  try {
    const res = await fetch(`${API}/incidents?status=open`, { credentials: 'same-origin' });
    if (res.ok) {
      const incidents = await res.json();
      if (incidents.length > 0) {
        alerts.push({
          level: 'danger',
          title: `${incidents.length} open incident${incidents.length > 1 ? 's' : ''}`,
          text: 'Require administrator attention',
          bg: '#FEE2E2', color: '#991B1B'
        });
      }
    }
  } catch (e) { /* ignore */ }

  // Always show audit logging status
  alerts.push({
    level: 'info',
    title: 'Audit logging active',
    text: 'All significant administrative actions are being recorded.',
    bg: '#EFF6FF', color: '#1E40AF'
  });

  const el = document.getElementById('systemAlerts');
  el.innerHTML = alerts.map(a => `
    <div style="background:${a.bg};color:${a.color};padding:12px 16px;border-radius:10px;margin-bottom:10px">
      <strong style="display:block;margin-bottom:3px">${escapeHtml(a.title)}</strong>
      <span style="font-size:0.85rem">${escapeHtml(a.text)}</span>
    </div>
  `).join('');
}

document.addEventListener('DOMContentLoaded', async () => {
  // User chip — read from localStorage set at login
  let user = null;
  try { user = JSON.parse(localStorage.getItem('saferide.user') || 'null'); } catch (e) {}
  if (user) {
    document.getElementById('userChipName').textContent = user.fullName || 'System Administrator';
    document.getElementById('userChipAvatar').textContent = initials(user.fullName);
  }

  await loadStats();
  await loadRecentActivity();
  await loadSystemAlerts();

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
