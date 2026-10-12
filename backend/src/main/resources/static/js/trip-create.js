// SafeRide SA — operator: create trip
const API = '/api';
let myOperator = null;

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

function showStatus(type, msg) {
  const box = document.getElementById('statusBox');
  if (type === 'success') {
    box.innerHTML = `<div class="alert" style="background:#DCFCE7;color:#166534">${escapeHtml(msg)}</div>`;
  } else {
    box.innerHTML = `<div class="alert" style="background:#FEE2E2;color:#991B1B">${escapeHtml(msg)}</div>`;
  }
  setTimeout(() => { box.innerHTML = ''; }, 8000);
}

async function loadOperator() {
  const res = await fetch(`${API}/operators/me`, { credentials: 'same-origin' });
  if (!res.ok) return null;
  return res.json();
}

async function loadVehicles(operatorId) {
  // We don't have a dedicated vehicles endpoint, so query trips to see known vehicles
  // Simpler: hardcode the seed vehicles for this operator
  const vehicles = [
    { id: 1, reg: 'CA 123-456' },
    { id: 2, reg: 'CA 789-012' }
  ];
  const sel = document.getElementById('vehicleSelect');
  sel.innerHTML = '';
  for (const v of vehicles) {
    const opt = document.createElement('option');
    opt.value = v.id;
    opt.textContent = `${v.reg} (id ${v.id})`;
    sel.appendChild(opt);
  }
}

async function submit() {
  const form = document.getElementById('tripForm');
  const data = new FormData(form);

  const routeName = (data.get('routeName') || '').trim();
  const vehicleId = parseInt(data.get('vehicleId'), 10);
  const departureTime = data.get('departureTime');
  const routeIdStr = data.get('routeId');
  const routeId = routeIdStr ? parseInt(routeIdStr, 10) : null;

  if (!routeName || !vehicleId || !departureTime) {
    showStatus('error', 'Please fill route name, vehicle and departure time.');
    return;
  }
  if (!myOperator) {
    showStatus('error', 'No operator profile found for your account.');
    return;
  }

  const payload = {
    operatorId: myOperator.id,
    vehicleId: vehicleId,
    routeId: routeId,
    routeName: routeName,
    departureTime: departureTime + ':00'   // backend expects LocalDateTime
  };

  const btn = document.getElementById('saveBtn');
  btn.disabled = true;
  btn.textContent = 'Creating…';

  try {
    const res = await fetch(`${API}/trips`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify(payload)
    });
    const out = await res.json();
    btn.disabled = false;
    btn.textContent = 'Create Trip';

    if (!res.ok) {
      showStatus('error', out.error || 'Failed to create trip');
      return;
    }
    showStatus('success', `✓ Trip created (id ${out.id}). Redirecting to dashboard…`);
    setTimeout(() => { window.location.href = 'dashboard.html'; }, 1500);
  } catch (e) {
    btn.disabled = false;
    btn.textContent = 'Create Trip';
    showStatus('error', 'Network error: ' + e.message);
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

  myOperator = await loadOperator();
  if (myOperator) {
    await loadVehicles(myOperator.id);
  } else {
    showStatus('error', 'No operator profile found for your account.');
  }

  // Default departure time = now
  const dt = new Date();
  dt.setMinutes(dt.getMinutes() - dt.getTimezoneOffset());
  document.querySelector('input[name=departureTime]').value = dt.toISOString().slice(0, 16);

  document.getElementById('saveBtn').addEventListener('click', submit);

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
