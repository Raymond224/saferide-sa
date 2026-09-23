// SafeRide SA — login form handler
document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();

  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value;
  const errBox = document.getElementById('err');
  const btn = document.getElementById('loginBtn');

  errBox.classList.remove('show');
  btn.disabled = true;
  btn.textContent = 'Signing in…';

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'same-origin',
      body: JSON.stringify({ username, password })
    });

    const data = await res.json();

    if (!res.ok) {
      errBox.textContent = data.error || 'Login failed';
      errBox.classList.add('show');
      btn.disabled = false;
      btn.textContent = 'Sign in';
      return;
    }

    localStorage.setItem('saferide.user', JSON.stringify(data));

    const redirects = {
      'parent':        'parent/dashboard.html',
      'operator':      'operator/dashboard.html',
      'admin':         'admin/dashboard.html',
      'system-admin':  'system-admin/dashboard.html'
    };

    const dest = redirects[data.role];
    if (dest) {
      window.location.href = dest;
    } else {
      errBox.textContent = 'Unknown role: ' + data.role;
      errBox.classList.add('show');
      btn.disabled = false;
      btn.textContent = 'Sign in';
    }

  } catch (err) {
    errBox.textContent = 'Network error. Is the backend running?';
    errBox.classList.add('show');
    btn.disabled = false;
    btn.textContent = 'Sign in';
  }
});
