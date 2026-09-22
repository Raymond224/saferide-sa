-- SafeRide SA — Database Schema (SQLite)
-- Learner transport safety system for Kimberley schools

PRAGMA foreign_keys = ON;

-- ============================================================
-- SCHOOLS
-- ============================================================
CREATE TABLE schools (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT NOT NULL,
    address       TEXT,
    phone         TEXT,
    email         TEXT,
    created_at    TEXT DEFAULT (datetime('now'))
);

-- ============================================================
-- USERS (parents, operators, admins, system admins)
-- ============================================================
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    role          TEXT NOT NULL CHECK(role IN ('parent','operator','admin','system-admin')),
    full_name     TEXT NOT NULL,
    email         TEXT,
    phone         TEXT,
    school_id     INTEGER,
    active        INTEGER NOT NULL DEFAULT 1,
    created_at    TEXT DEFAULT (datetime('now')),
    updated_at    TEXT DEFAULT (datetime('now')),
    deleted_at    TEXT,
    FOREIGN KEY (school_id) REFERENCES schools(id)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_school ON users(school_id);

-- ============================================================
-- LEARNERS (children)
-- ============================================================
CREATE TABLE learners (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name     TEXT NOT NULL,
    grade         TEXT,
    parent_id     INTEGER NOT NULL,
    school_id     INTEGER NOT NULL,
    created_at    TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (parent_id) REFERENCES users(id),
    FOREIGN KEY (school_id) REFERENCES schools(id)
);

CREATE INDEX idx_learners_parent ON learners(parent_id);
CREATE INDEX idx_learners_school ON learners(school_id);

-- ============================================================
-- OPERATORS (transport companies)
-- ============================================================
CREATE TABLE operators (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                INTEGER NOT NULL,
    company_name           TEXT NOT NULL,
    school_id              INTEGER NOT NULL,
    prdp_expiry            TEXT,
    roadworthy_expiry      TEXT,
    registration_expiry    TEXT,
    active                 INTEGER NOT NULL DEFAULT 1,
    created_at             TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (school_id) REFERENCES schools(id)
);

CREATE INDEX idx_operators_school ON operators(school_id);

-- ============================================================
-- VEHICLES
-- ============================================================
CREATE TABLE vehicles (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id         INTEGER NOT NULL,
    registration_number TEXT NOT NULL,
    make                TEXT,
    model               TEXT,
    capacity            INTEGER,
    created_at          TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (operator_id) REFERENCES operators(id)
);

CREATE INDEX idx_vehicles_operator ON vehicles(operator_id);

-- ============================================================
-- ROUTES
-- ============================================================
CREATE TABLE routes (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id    INTEGER NOT NULL,
    name           TEXT NOT NULL,
    waypoints      TEXT,
    created_at     TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (operator_id) REFERENCES operators(id)
);

-- ============================================================
-- TRIPS
-- ============================================================
CREATE TABLE trips (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id     INTEGER NOT NULL,
    vehicle_id      INTEGER NOT NULL,
    route_id        INTEGER,
    route_name      TEXT,
    departure_time  TEXT,
    arrival_time    TEXT,
    status          TEXT NOT NULL DEFAULT 'scheduled'
                    CHECK(status IN ('scheduled','on-route','delayed','deviated','completed')),
    current_lat     REAL,
    current_lng     REAL,
    created_at      TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (operator_id) REFERENCES operators(id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    FOREIGN KEY (route_id) REFERENCES routes(id)
);

CREATE INDEX idx_trips_operator ON trips(operator_id);
CREATE INDEX idx_trips_status ON trips(status);

-- ============================================================
-- TRIP_LEARNERS (tick-box register)
-- ============================================================
CREATE TABLE trip_learners (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    trip_id         INTEGER NOT NULL,
    learner_id      INTEGER NOT NULL,
    picked_up       INTEGER NOT NULL DEFAULT 0,
    dropped_off     INTEGER NOT NULL DEFAULT 0,
    picked_up_at    TEXT,
    dropped_off_at  TEXT,
    FOREIGN KEY (trip_id) REFERENCES trips(id),
    FOREIGN KEY (learner_id) REFERENCES learners(id),
    UNIQUE (trip_id, learner_id)
);

CREATE INDEX idx_trip_learners_trip ON trip_learners(trip_id);

-- ============================================================
-- INCIDENTS
-- ============================================================
CREATE TABLE incidents (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    reported_by     INTEGER NOT NULL,
    trip_id         INTEGER,
    type            TEXT NOT NULL,
    description     TEXT,
    status          TEXT NOT NULL DEFAULT 'open'
                    CHECK(status IN ('open','investigating','resolved')),
    created_at      TEXT DEFAULT (datetime('now')),
    resolved_at     TEXT,
    FOREIGN KEY (reported_by) REFERENCES users(id),
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);

CREATE INDEX idx_incidents_status ON incidents(status);

-- ============================================================
-- MESSAGES (parent <-> school)
-- ============================================================
CREATE TABLE messages (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    from_user       INTEGER NOT NULL,
    to_school       INTEGER NOT NULL,
    content         TEXT NOT NULL,
    reply_to        INTEGER,
    read_at         TEXT,
    created_at      TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (from_user) REFERENCES users(id),
    FOREIGN KEY (to_school) REFERENCES schools(id),
    FOREIGN KEY (reply_to) REFERENCES messages(id)
);

CREATE INDEX idx_messages_school ON messages(to_school);

-- ============================================================
-- COMPLIANCE_DOCUMENTS
-- ============================================================
CREATE TABLE compliance_documents (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    operator_id   INTEGER NOT NULL,
    doc_type      TEXT NOT NULL CHECK(doc_type IN ('prdp','roadworthy','registration')),
    expiry_date   TEXT NOT NULL,
    file_url      TEXT,
    created_at    TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (operator_id) REFERENCES operators(id)
);

CREATE INDEX idx_compliance_operator ON compliance_documents(operator_id);

-- ============================================================
-- AUDIT_LOG
-- ============================================================
CREATE TABLE audit_log (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER,
    action        TEXT NOT NULL,
    details       TEXT,
    created_at    TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);
