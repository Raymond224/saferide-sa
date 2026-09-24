// SafeRide SA — admin messages page
const API = '/api';
let allMessages = [];
let activeMessage = null;

function currentUser() {
  try { return JSON.parse(localStorage.getItem('saferide.user') || 'null'); }
  catch { return null; }
}
function requireLogin() {
  const u = currentUser();
  if (!u) { window.location.href = '/index.html'; return null; }
  return u;
}

function formatDate(iso) {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

function escapeHtml(s) {
  if (!s) return '';
  return s.replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]));
}

async function loadSummary() {
  const res = await fetch(`${API}/messages/summary`, { credentials: 'same-origin' });
  if (res.status === 401) { window.location.href = '/index.html'; return; }
  const s = await res.json();
  document.getElementById('statTotal').textContent = s.total;
  document.getElementById('statUnread').textContent = s.unread;
  document.getElementById('statRead').textContent = s.total - s.unread;
  document.getElementById('statRate').textContent =
    s.total > 0 ? Math.round((s.total - s.unread) / s.total * 100) + '%' : '—';
}

async function loadMessages() {
  const res = await fetch(`${API}/messages`, { credentials: 'same-origin' });
  if (!res.ok) return;
  allMessages = await res.json();
  renderInbox(allMessages);
}

function renderInbox(messages) {
  const el = document.getElementById('messagesList');
  if (!messages || messages.length === 0) {
    el.innerHTML = '<p class="empty">No messages</p>';
    return;
  }

  el.innerHTML = messages.map(m => `
    <div class="activity" data-msg-id="${m.id}" style="cursor:pointer;${m.readAt ? '' : 'font-weight:600'}">
      <div class="dot" style="background:${m.readAt ? 'var(--muted)' : 'var(--danger)'}"></div>
      <div style="flex:1">
        <div style="display:flex;justify-content:space-between;align-items:baseline">
          <strong>${escapeHtml(m.fromUserName || 'Unknown')}</strong>
          <small style="color:var(--muted)">${formatDate(m.createdAt)}</small>
        </div>
        <small style="color:var(--muted);display:block;margin-top:3px">
          ${escapeHtml(m.content.length > 80 ? m.content.slice(0, 80) + '…' : m.content)}
        </small>
        ${m.replyTo ? '<small style="color:var(--accent);display:block;margin-top:3px">↳ Reply to #' + m.replyTo + '</small>' : ''}
      </div>
    </div>
  `).join('');

  // Click handler for each row
  el.querySelectorAll('[data-msg-id]').forEach(row => {
    row.addEventListener('click', () => openReply(row.dataset.msgId));
  });
}

async function openReply(messageId) {
  const res = await fetch(`${API}/messages/${messageId}`, { credentials: 'same-origin' });
  if (!res.ok) { alert('Failed to load message'); return; }
  const msg = await res.json();
  activeMessage = msg;

  document.getElementById('replyPane').innerHTML = `
    <div style="margin-bottom:16px">
      <strong>From:</strong> ${escapeHtml(msg.fromUserName || 'Unknown')}
      <small style="color:var(--muted)"> (${msg.fromUserRole || 'unknown'})</small><br>
      <strong>Received:</strong> ${formatDate(msg.createdAt)}<br>
      ${msg.readAt ? '<span class="badge badge-green" style="margin-top:6px">Read</span>' :
                     '<span class="badge badge-red" style="margin-top:6px">Unread</span>'}
    </div>
    <div style="background:var(--bg);padding:14px;border-radius:8px;margin-bottom:16px">
      ${escapeHtml(msg.content).replace(/\n/g, '<br>')}
    </div>
    <div class="form-group">
      <label>Reply</label>
      <textarea id="replyText" class="input" rows="5"
                style="width:100%;min-height:120px;resize:vertical;font-family:inherit"
                placeholder="Type your reply…"></textarea>
    </div>
    <div class="form-actions">
      <button class="btn btn-outline" id="cancelReply">Cancel</button>
      <button class="btn btn-primary" id="sendReply">Send Reply</button>
    </div>
  `;

  document.getElementById('cancelReply').addEventListener('click', () => {
    document.getElementById('replyPane').innerHTML = '<p class="empty">Select a message to reply</p>';
  });

  document.getElementById('sendReply').addEventListener('click', async () => {
    const content = document.getElementById('replyText').value.trim();
    if (!content) { alert('Please type a reply'); return; }

    const btn = document.getElementById('sendReply');
    btn.disabled = true;
    btn.textContent = 'Sending…';

    const res = await fetch(`${API}/messages/${messageId}/reply`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ content })
    });
    const data = await res.json();

    btn.disabled = false;
    btn.textContent = 'Send Reply';

    if (res.ok) {
      alert('✓ Reply sent');
      document.getElementById('replyPane').innerHTML = '<p class="empty">Reply sent. Select another message.</p>';
      await loadMessages();
      await loadSummary();
    } else {
      alert('Failed: ' + (data.error || 'unknown'));
    }
  });
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
  await loadMessages();

  // Filter
  document.getElementById('readFilter').addEventListener('change', (e) => {
    const v = e.target.value;
    let filtered = allMessages;
    if (v === 'unread') filtered = allMessages.filter(m => !m.readAt);
    if (v === 'read')   filtered = allMessages.filter(m =>  m.readAt);
    renderInbox(filtered);
  });

  // Logout
  document.querySelector('.logout-link').addEventListener('click', async (e) => {
    e.preventDefault();
    await fetch(`${API}/auth/logout`, { method: 'POST', credentials: 'same-origin' });
    localStorage.removeItem('saferide.user');
    window.location.href = '/index.html';
  });
});
