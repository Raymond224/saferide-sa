// SafeRide SA — system-admin school management
const API = '/api';

function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

let allSchools = [];

async function loadSchools() {
  const res = await fetch(`${API}/schools`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  if (!res.ok) {
    document.getElementById('schoolTable').innerHTML =
      '<tr><td colspan="6" class="empty">Failed to load schools</td></tr>';
    return;
  }
  allSchools = await res.json();
  renderSchools(allSchools);
}

function renderSchools(schools) {
  const tbody = document.getElementById('schoolTable');
  tbody.innerHTML = '';

  if (schools.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty">No schools</td></tr>';
    return;
  }

  for (const s of schools) {
    const created = s.createdAt ? s.createdAt.slice(0, 10) : '—';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong>${escapeHtml(s.name)}</strong></td>
      <td>${escapeHtml(s.address || '—')}</td>
      <td>${escapeHtml(s.phone || '—')}</td>
      <td>${escapeHtml(s.email || '—')}</td>
      <td>${s.userCount != null ? s.userCount : '—'}</td>
      <td>${escapeHtml(created)}</td>
    `;
    tbody.appendChild(tr);
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  await loadSchools();

  // Search filter (client-side)
  document.getElementById('schoolSearch').addEventListener('input', (e) => {
    const q = e.target.value.toLowerCase();
    const filtered = allSchools.filter(s =>
      (s.name || '').toLowerCase().includes(q) ||
      (s.address || '').toLowerCase().includes(q) ||
      (s.email || '').toLowerCase().includes(q)
    );
    renderSchools(filtered);
  });

  // Add School button (placeholder for now — we can wire a modal later)
  document.getElementById('btnAddSchool').addEventListener('click', () => {
    alert('Add School form coming soon. Use the API: POST /api/schools');
  });

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
