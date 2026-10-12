// SafeRide SA — system-admin user management
const API = '/api';

function escapeHtml(s) {
  if (s == null) return '';
  return String(s).replace(/[&<>"']/g, c => ({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  }[c]));
}

// Map backend role → display label + data-role attribute
function roleLabel(role) {
  const map = {
    'parent':       'Parent',
    'operator':     'Operator',
    'admin':        'School Admin',
    'system-admin': 'System Admin'
  };
  return map[role] || role;
}

async function loadUsers() {
  const res = await fetch(`${API}/users`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  if (res.status === 403) {
    document.getElementById('userTable').innerHTML =
      '<tr><td colspan="6" class="empty">Not authorized to view users</td></tr>';
    return;
  }
  const users = await res.json();
  renderUsers(users);
}

function renderUsers(users) {
  const tbody = document.getElementById('userTable');
  tbody.innerHTML = '';

  if (users.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty">No users</td></tr>';
    return;
  }

  for (const u of users) {
    const label = roleLabel(u.role);
    const status = u.active ? 'Active' : 'Suspended';
    const statusBadge = u.active
      ? '<span class="badge badge-green">Active</span>'
      : '<span class="badge badge-red">Suspended</span>';
    const actionLabel = u.active ? 'Suspend' : 'Activate';
    const created = u.createdAt ? u.createdAt.slice(0, 10) : '—';

    const tr = document.createElement('tr');
    tr.dataset.role = label;
    tr.dataset.status = status;
    tr.dataset.userId = u.id;

    tr.innerHTML = `
      <td>${escapeHtml(u.fullName)}</td>
      <td>${escapeHtml(u.email || '—')}</td>
      <td>${escapeHtml(label)}</td>
      <td>${statusBadge}</td>
      <td>${escapeHtml(created)}</td>
      <td><button class="btn btn-outline btn-sm" onclick="toggleStatus(this)">${actionLabel}</button></td>
    `;
    tbody.appendChild(tr);
  }
}

// Override Phiwe's toggleStatus — call the backend instead of just flipping the DOM
window.toggleStatus = async function (btn) {
  const row = btn.closest('tr');
  const userId = row.dataset.userId;
  if (!userId) { return; } // safe no-op if no data-user-id

  const wasActive = row.dataset.status === 'Active';
  const newActive = !wasActive;

  btn.disabled = true;
  btn.textContent = 'Saving…';

  try {
    const res = await fetch(`${API}/users/${userId}/active`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ active: newActive })
    });
    const data = await res.json();

    if (!res.ok) {
      alert('Failed: ' + (data.error || 'unknown error'));
      btn.disabled = false;
      btn.textContent = wasActive ? 'Suspend' : 'Activate';
      return;
    }

    // Update the DOM to reflect new state
    row.dataset.status = newActive ? 'Active' : 'Suspended';
    const badge = row.querySelector('.badge');
    badge.textContent = newActive ? 'Active' : 'Suspended';
    badge.className = 'badge ' + (newActive ? 'badge-green' : 'badge-red');
    btn.textContent = newActive ? 'Suspend' : 'Activate';
    btn.disabled = false;
  } catch (err) {
    alert('Network error');
    btn.disabled = false;
    btn.textContent = wasActive ? 'Suspend' : 'Activate';
  }
};

document.addEventListener('DOMContentLoaded', loadUsers);
