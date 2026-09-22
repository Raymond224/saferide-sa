-- SafeRide SA — Seed Data (SQLite)
-- All demo passwords are: password123
-- bcrypt hash below is the hash of "password123"

PRAGMA foreign_keys = ON;

-- ============================================================
-- SCHOOLS
-- ============================================================
INSERT INTO schools (id, name, address, phone, email) VALUES
(1, 'Galeshewe High School', '123 Main Rd, Galeshewe, Kimberley', '053-123-4567', 'admin@galeshewehigh.co.za'),
(2, 'Kimberley Boys High',   '45 School St, Kimberley',          '053-234-5678', 'info@kbh.co.za'),
(3, 'Diamantveld High',      '78 Diamond Ave, Kimberley',        '053-345-6789', 'contact@diamantveld.co.za');

-- ============================================================
-- USERS
-- ============================================================
INSERT INTO users (id, username, password_hash, role, full_name, email, phone, school_id) VALUES
(1, 'sysadmin', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'system-admin', 'Phiwe Nkosi',       'phiwe@saferide.co.za',    '072-111-1111', NULL),
(2, 'admin',    '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin',        'Raymond Dlamini',   'raymond@galeshewe.co.za', '072-222-2222', 1),
(3, 'admin2',   '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin',        'Sarah Mokoena',     'sarah@kbh.co.za',         '072-222-3333', 2),
(4, 'admin3',   '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin',        'Peter Nkosi',       'peter@diamantveld.co.za', '072-222-4444', 3),
(5, 'parent',   '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'parent',       'Puleng Mokoena',    'puleng@gmail.com',        '072-333-3333', 1),
(6, 'parent2',  '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'parent',       'Thabo Nkosi',       'thabo@gmail.com',         '072-333-4444', 1),
(7, 'parent3',  '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'parent',       'Lerato Mahlangu',   'lerato@gmail.com',        '072-333-5555', 1),
(8, 'parent4',  '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'parent',       'Sipho Zulu',        'sipho@gmail.com',         '072-333-6666', 2),
(9, 'parent5',  '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'parent',       'Naledi Khumalo',    'naledi@gmail.com',        '072-333-7777', 1),
(10,'operator', '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'operator',     'Qetelo Sithole',    'qetelo@tours.co.za',      '072-444-4444', 1),
(11,'operator2','$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'operator',     'Jacob Mahlangu',    'jacob@diamond.co.za',     '072-444-5555', 1),
(12,'operator3','$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'operator',     'David Omiwole',     'david@kidsrides.co.za',   '072-444-6666', 1),
(13,'operator4','$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'operator',     'Michael Lukayi',    'michael@nkosi.co.za',     '072-444-7777', 1);

-- ============================================================
-- LEARNERS
-- ============================================================
INSERT INTO learners (id, full_name, grade, parent_id, school_id) VALUES
(1, 'Thabo Mokoena',    'Grade 8', 5, 1),
(2, 'Naledi Mokoena',   'Grade 6', 5, 1),
(3, 'Kabelo Nkosi',     'Grade 9', 6, 1),
(4, 'Amogelang Mahlangu','Grade 7', 7, 1),
(5, 'Bokang Zulu',      'Grade 10',8, 2),
(6, 'Zanele Khumalo',   'Grade 8', 9, 1);

-- ============================================================
-- OPERATORS
-- NOTE: Galeshewe Tours (id=1) has an EXPIRED roadworthy
-- ============================================================
INSERT INTO operators (id, user_id, company_name, school_id, prdp_expiry, roadworthy_expiry, registration_expiry) VALUES
(1, 10, 'Galeshewe Tours',      1, '2025-08-12', '2025-01-05', '2025-11-30'),
(2, 11, 'Diamond Transport',    1, '2025-09-20', '2025-10-15', '2025-12-31'),
(3, 12, 'Kimberley Kids Rides', 1, '2025-10-08', '2026-01-20', '2026-03-15'),
(4, 13, 'Nkosi Shuttle Services', 1, '2025-11-01','2025-12-15', '2026-02-28');

-- ============================================================
-- VEHICLES
-- ============================================================
INSERT INTO vehicles (id, operator_id, registration_number, make, model, capacity) VALUES
(1, 1, 'CA 123-456', 'Toyota',   'Quantum',   15),
(2, 1, 'CA 789-012', 'Mercedes', 'Sprinter',  22),
(3, 2, 'CA 456-789', 'Toyota',   'Quantum',   15),
(4, 2, 'CA 234-567', 'Ford',     'Transit',   12),
(5, 3, 'CA 987-654', 'Toyota',   'HiAce',     14),
(6, 4, 'CA 555-111', 'Nissan',   'NV350',     16);

-- ============================================================
-- ROUTES
-- ============================================================
INSERT INTO routes (id, operator_id, name, waypoints) VALUES
(1, 1, 'Galeshewe Route A', '[[-28.7361,24.7619],[-28.7400,24.7700],[-28.7450,24.7800]]'),
(2, 1, 'Galeshewe Route B', '[[-28.7361,24.7619],[-28.7300,24.7500],[-28.7250,24.7450]]'),
(3, 2, 'Diamond Route C',   '[[-28.7500,24.7900],[-28.7450,24.7950],[-28.7400,24.8000]]'),
(4, 3, 'Kids Route D',      '[[-28.7200,24.7400],[-28.7150,24.7450],[-28.7100,24.7500]]');

-- ============================================================
-- TRIPS
-- ============================================================
INSERT INTO trips (id, operator_id, vehicle_id, route_id, route_name, departure_time, arrival_time, status, current_lat, current_lng) VALUES
(1, 1, 1, 1, 'Galeshewe Route A', '2026-09-22 06:30:00', NULL, 'on-route',  -28.7400, 24.7700),
(2, 1, 2, 2, 'Galeshewe Route B', '2026-09-22 06:45:00', NULL, 'delayed',   -28.7300, 24.7500),
(3, 2, 3, 3, 'Diamond Route C',   '2026-09-22 06:30:00', NULL, 'deviated',  -28.7420, 24.7980),
(4, 3, 5, 4, 'Kids Route D',      '2026-09-22 06:45:00', NULL, 'on-route',  -28.7150, 24.7450),
(5, 4, 6, NULL, 'Nkosi Morning Run','2026-09-21 06:30:00','2026-09-21 07:45:00','completed', NULL, NULL);

-- ============================================================
-- TRIP_LEARNERS (tick-box register)
-- ============================================================
INSERT INTO trip_learners (trip_id, learner_id, picked_up, dropped_off, picked_up_at, dropped_off_at) VALUES
(1, 1, 1, 0, '2026-09-22 06:35:00', NULL),
(1, 2, 1, 0, '2026-09-22 06:37:00', NULL),
(1, 6, 0, 0, NULL, NULL),
(2, 4, 1, 0, '2026-09-22 06:50:00', NULL),
(3, 3, 1, 0, '2026-09-22 06:35:00', NULL),
(4, 5, 1, 0, '2026-09-22 06:50:00', NULL),
(5, 1, 1, 1, '2026-09-21 06:35:00', '2026-09-21 07:40:00'),
(5, 2, 1, 1, '2026-09-21 06:37:00', '2026-09-21 07:42:00');

-- ============================================================
-- INCIDENTS
-- ============================================================
INSERT INTO incidents (id, reported_by, trip_id, type, description, status, created_at) VALUES
(1, 5, 3, 'route-deviation', 'Vehicle took an unregistered turn near the taxi rank. My child was on board.', 'open', '2026-09-22 07:42:00'),
(2, 10, 2, 'breakdown', 'Vehicle broke down on Route B. Learners transferred to backup vehicle.', 'investigating', '2026-09-21 16:10:00'),
(3, 5, 1, 'late-pickup', 'Driver arrived 20 minutes late for morning pickup.', 'resolved', '2026-09-19 08:05:00');

-- ============================================================
-- MESSAGES (parent <-> school)
-- ============================================================
INSERT INTO messages (id, from_user, to_school, content, read_at, created_at) VALUES
(1, 5, 1, 'My child was not picked up this morning. Route A.', NULL, '2026-09-22 07:55:00'),
(2, 6, 1, 'Can I get a copy of the trip history for last week?', NULL, '2026-09-22 09:15:00'),
(3, 7, 1, 'Thank you for the quick response yesterday.', '2026-09-21 14:00:00', '2026-09-21 13:30:00'),
(4, 8, 2, 'Please confirm the new route for next week.', NULL, '2026-09-20 15:00:00'),
(5, 9, 1, 'The bus was very full this morning. Is this normal?', NULL, '2026-09-20 08:30:00');

-- ============================================================
-- COMPLIANCE_DOCUMENTS
-- ============================================================
INSERT INTO compliance_documents (operator_id, doc_type, expiry_date) VALUES
-- Galeshewe Tours (operator 1) — roadworthy EXPIRED
(1, 'prdp',         '2025-08-12'),
(1, 'roadworthy',   '2025-01-05'),
(1, 'registration', '2025-11-30'),
-- Diamond Transport (operator 2) — all valid
(2, 'prdp',         '2025-09-20'),
(2, 'roadworthy',   '2025-10-15'),
(2, 'registration', '2025-12-31'),
-- Kimberley Kids Rides (operator 3) — prdp expiring soon
(3, 'prdp',         '2025-10-08'),
(3, 'roadworthy',   '2026-01-20'),
(3, 'registration', '2026-03-15'),
-- Nkosi Shuttle Services (operator 4) — all valid
(4, 'prdp',         '2025-11-01'),
(4, 'roadworthy',   '2025-12-15'),
(4, 'registration', '2026-02-28');

-- ============================================================
-- AUDIT_LOG
-- ============================================================
INSERT INTO audit_log (user_id, action, details) VALUES
(2, 'login',            'Admin Raymond Dlamini logged in'),
(10, 'trip-created',    'Trip 1 created on Route A'),
(5, 'incident-reported','Incident 1: route deviation on Route A'),
(2, 'incident-updated', 'Incident 2 status changed to investigating'),
(1, 'user-created',     'System admin created operator account for Kimberley Kids Rides');
