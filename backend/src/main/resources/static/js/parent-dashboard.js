// SafeRide SA - Parent Dashboard

const API = '/api';

function currentUser() {
  try {
    return JSON.parse(
      localStorage.getItem('saferide.user') || 'null'
    );
  } catch {
    return null;
  }
}

function requireLogin() {
  const user = currentUser();

  if (!user) {
    window.location.href = '/index.html';
    return null;
  }

  return user;
}

function initials(name) {
  return (name || '')
    .split(' ')
    .filter(Boolean)
    .map(word => word[0])
    .slice(0, 2)
    .join('')
    .toUpperCase();
}

function formatDate(iso) {
  if (!iso) {
    return '--';
  }

  return iso
    .replace('T', ' ')
    .slice(0, 16);
}

function tripBadge(status) {
  const map = {
    'on-route': ['badge-green', 'On Route'],
    'delayed': ['badge-red', 'Delayed'],
    'deviated': ['badge-red', 'Deviated'],
    'completed': ['badge-blue', 'Completed'],
    'scheduled': ['badge-blue', 'Scheduled']
  };

  const [className, label] =
    map[status] || ['badge-blue', status || '--'];

  return `
    <span class="badge ${className}">
      ${label}
    </span>
  `;
}

/*
 * Converts a learner's current trip information
 * into a parent-friendly status.
 */
function learnerBadge(learner) {
  const status = learner.currentTripStatus;

  if (!learner.currentTripId) {
    return `
      <span class="badge badge-blue">
        No trip scheduled
      </span>
    `;
  }

  if (learner.droppedOff) {
    return `
      <span class="badge badge-blue">
        Dropped Off
      </span>
    `;
  }

  if (learner.pickedUp) {

    if (status === 'delayed') {
      return `
        <span class="badge badge-red">
          Picked Up - Delayed
        </span>
      `;
    }

    if (status === 'deviated') {
      return `
        <span class="badge badge-red">
          Picked Up - Deviated
        </span>
      `;
    }

    return `
      <span class="badge badge-green">
        En Route
      </span>
    `;
  }

  if (status === 'delayed') {
    return `
      <span class="badge badge-red">
        Delayed
      </span>
    `;
  }

  if (status === 'deviated') {
    return `
      <span class="badge badge-red">
        Deviated
      </span>
    `;
  }

  if (status === 'scheduled') {
    return `
      <span class="badge badge-blue">
        Scheduled
      </span>
    `;
  }

  return `
    <span class="badge badge-blue">
      ${status || 'Unknown'}
    </span>
  `;
}


/* -----------------------------
   Load parent's children
----------------------------- */

async function loadChildren() {

  const element =
    document.getElementById('childrenList');

  try {

    const response = await fetch(
      `${API}/learners/mine`,
      {
        credentials: 'same-origin'
      }
    );

    if (response.status === 401) {
      window.location.href = '/index.html';
      return;
    }

    if (!response.ok) {
      throw new Error('Could not load children');
    }

    const learners = await response.json();

    document.getElementById(
      'statChildren'
    ).textContent = learners.length;

    if (learners.length === 0) {

      element.innerHTML = `
        <p class="empty">
          No children registered yet.
        </p>
      `;

      return;
    }

    element.innerHTML = learners.map(learner => `

      <div class="child-card">

        <div
          style="
            display:flex;
            justify-content:space-between;
            align-items:flex-start;
            gap:12px;
          "
        >

          <div>

            <div class="name">
              ${learner.fullName || 'Unknown'}
            </div>

            <div class="meta">
              ${learner.grade || '--'}
              &middot;
              ${learner.schoolName || '--'}
            </div>

            ${
              learner.currentRouteName
                ? `
                  <div class="meta">
                    Route: ${learner.currentRouteName}
                  </div>
                `
                : ''
            }

          </div>

          ${learnerBadge(learner)}

        </div>

      </div>

    `).join('');

  } catch (error) {

    console.error(
      'Error loading children:',
      error
    );

    element.innerHTML = `
      <p class="empty">
        Could not load your children.
      </p>
    `;

    document.getElementById(
      'statChildren'
    ).textContent = '--';
  }
}


/* -----------------------------
   Load active trips
----------------------------- */

async function loadActiveTrips() {

  const element =
    document.getElementById('activeTripsList');

  try {

    const response = await fetch(
      `${API}/trips/mine?status=active`,
      {
        credentials: 'same-origin'
      }
    );

    if (response.status === 401) {
      window.location.href = '/index.html';
      return;
    }

    if (!response.ok) {
      throw new Error('Could not load trips');
    }

    const trips = await response.json();

    document.getElementById(
      'statActiveTrips'
    ).textContent = trips.length;

    if (trips.length === 0) {

      element.innerHTML = `
        <p class="empty">
          No active trips right now.
        </p>
      `;

      return;
    }

    element.innerHTML = trips.map(trip => `

      <div class="activity">

        <div class="dot"></div>

        <div style="flex:1">

          <div
            style="
              display:flex;
              justify-content:space-between;
              align-items:baseline;
              gap:10px;
            "
          >

            <strong>
              ${trip.routeName || `Trip #${trip.id}`}
            </strong>

            ${tripBadge(trip.statusString)}

          </div>

          <small
            style="
              color:var(--muted);
              display:block;
              margin-top:3px;
            "
          >
            ${trip.operatorName || '--'}
            &middot;
            ${trip.vehicleRegistration || '--'}
            &middot;
            Departed ${formatDate(trip.departureTime)}
          </small>

        </div>

      </div>

    `).join('');

  } catch (error) {

    console.error(
      'Error loading active trips:',
      error
    );

    element.innerHTML = `
      <p class="empty">
        Could not load active trips.
      </p>
    `;

    document.getElementById(
      'statActiveTrips'
    ).textContent = '--';
  }
}


/* -----------------------------
   Load open incidents
----------------------------- */

async function loadIncidentCount() {

  try {

    const response = await fetch(
      `${API}/incidents/mine`,
      {
        credentials: 'same-origin'
      }
    );

    if (!response.ok) {
      return;
    }

    const incidents =
      await response.json();

    const openIncidents =
      incidents.filter(
        incident =>
          incident.statusString === 'open'
      ).length;

    document.getElementById(
      'statIncidents'
    ).textContent = openIncidents;

  } catch (error) {

    console.error(
      'Error loading incidents:',
      error
    );
  }
}


/* -----------------------------
   Load unread messages
----------------------------- */

async function loadMessageCount() {

  try {

    const response = await fetch(
      `${API}/messages/summary`,
      {
        credentials: 'same-origin'
      }
    );

    if (!response.ok) {
      return;
    }

    const summary =
      await response.json();

    document.getElementById(
      'statMessages'
    ).textContent =
      summary.unread ?? 0;

  } catch (error) {

    console.error(
      'Error loading messages:',
      error
    );
  }
}


/* -----------------------------
   Page initialization
----------------------------- */

document.addEventListener(
  'DOMContentLoaded',
  async () => {

    const user = requireLogin();

    if (!user) {
      return;
    }

    const chip =
      document.querySelector('.user-chip');

    chip.innerHTML = `
      <div class="avatar">
        ${initials(user.fullName)}
      </div>

      <div>
        ${user.fullName || 'Parent'}
        <br>
        <small>${user.role || 'Parent'}</small>
      </div>
    `;

    const firstName =
      (user.fullName || 'Parent')
        .split(' ')[0];

    document.getElementById(
      'greetingName'
    ).textContent = firstName;

    await Promise.all([
      loadChildren(),
      loadActiveTrips(),
      loadIncidentCount(),
      loadMessageCount()
    ]);

    const logout =
      document.querySelector('.logout-link');

    logout.addEventListener(
      'click',
      async event => {

        event.preventDefault();

        try {

          await fetch(
            `${API}/auth/logout`,
            {
              method: 'POST',
              credentials: 'same-origin'
            }
          );

        } catch (error) {

          console.error(
            'Logout error:',
            error
          );
        }

        localStorage.removeItem(
          'saferide.user'
        );

        window.location.href =
          '/index.html';
      }
    );
  }
);