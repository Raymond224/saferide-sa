// SafeRide SA - parent messages page
const API = '/api';

function currentUser() {
  try {
    return JSON.parse(localStorage.getItem('saferide.user') || 'null');
  } catch {
    return null;
  }
}

function requireLogin() {
  const u = currentUser();

  if (!u) {
    window.location.href = '/index.html';
    return null;
  }

  return u;
}

function initials(name) {
  return (name || '')
    .split(' ')
    .map(w => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

function formatDate(iso) {
  if (!iso) return '—';
  return iso.replace('T', ' ').slice(0, 16);
}

function escapeHtml(s) {
  if (!s) return '';

  return s.replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[c]));
}

async function loadThread(user) {
  const el = document.getElementById('thread');

  try {
    const res = await fetch(`${API}/messages`, {
      credentials: 'same-origin'
    });

    if (res.status === 401) {
      window.location.href = '/index.html';
      return;
    }

    if (!res.ok) {
      el.innerHTML =
        '<p class="empty">Unable to load messages</p>';
      return;
    }

    const messages = await res.json();

    // API returns newest first.
    // Display the conversation oldest to newest.
    const ordered = [...messages].reverse();

    if (ordered.length === 0) {
      el.innerHTML =
        '<p class="empty">No messages yet. Say hello below!</p>';
      return;
    }

    el.innerHTML = ordered.map(m => {
      const mine = m.fromUser === user.id;

      return `
        <div class="bubble ${mine ? 'mine' : 'theirs'}">
          ${escapeHtml(m.content)}

          <small>
            ${mine ? 'You' : (m.fromUserName || 'School')}
            · ${formatDate(m.createdAt)}
          </small>
        </div>
      `;
    }).join('');

    el.scrollTop = el.scrollHeight;

    // Mark unread school replies as read.
    const unreadReplies = messages.filter(
      m => m.fromUser !== user.id && !m.readAt
    );

    for (const m of unreadReplies) {
      fetch(`${API}/messages/${m.id}`, {
        credentials: 'same-origin'
      }).catch(() => {});
    }
  } catch (error) {
    el.innerHTML =
      '<p class="empty">Unable to load messages</p>';
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const user = requireLogin();

  if (!user) return;

  const chip = document.querySelector('.user-chip');

  chip.innerHTML = `
    <div class="avatar">${initials(user.fullName)}</div>
    <div>
      ${user.fullName}<br>
      <small>${user.role}</small>
    </div>
  `;

  await loadThread(user);

  document.getElementById('sendBtn').addEventListener('click', async () => {
    const content =
      document.getElementById('composeText').value.trim();

    if (!content) {
      alert('Please type a message');
      return;
    }

    if (!user.schoolId) {
      alert('No school is linked to your account');
      return;
    }

    const btn = document.getElementById('sendBtn');

    btn.disabled = true;
    btn.textContent = 'Sending...';

    try {
      const res = await fetch(`${API}/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'same-origin',
        body: JSON.stringify({
          toSchool: user.schoolId,
          content
        })
      });

      const data = await res.json();

      if (res.ok) {
        document.getElementById('composeText').value = '';
        await loadThread(user);
      } else {
        alert('Failed: ' + (data.error || 'unknown error'));
      }
    } catch (error) {
      alert('Network error. Is the backend running?');
    } finally {
      btn.disabled = false;
      btn.textContent = 'Send';
    }
  });

  document.querySelector('.logout-link').addEventListener('click', async e => {
    e.preventDefault();

    await fetch(`${API}/auth/logout`, {
      method: 'POST',
      credentials: 'same-origin'
    });

    localStorage.removeItem('saferide.user');
    window.location.href = '/index.html';
  });
});