-- SafeRide SA — Database Schema (MySQL 8)
-- Learner transport safety system for Kimberley schools

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS compliance_documents;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS incidents;
DROP TABLE IF EXISTS trip_learners;
DROP TABLE IF EXISTS trips;
DROP TABLE IF EXISTS routes;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS operators;
DROP TABLE IF EXISTS learners;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS schools;
SET FOREIGN_KEY_CHECKS = 1;

-- SCHOOLS
CREATE TABLE schools (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(30),
    email       VARCHAR(150),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- USERS
CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           ENUM('parent','operator','admin','system-admin') NOT NULL,
    full_name      VARCHAR(150) NOT NULL,
    email          VARCHAR(150),
    phone          VARCHAR(30),
    school_id      BIGINT,
    active         TINYINT(1) NOT NULL DEFAULT 1,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP NULL,
    CONSTRAINT fk_users_school FOREIGN KEY (school_id) REFERENCES schools(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_role   ON users(role);
CREATE INDEX idx_users_school ON users(school_id);

-- LEARNERS
CREATE TABLE learners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    grade       VARCHAR(20),
    parent_id   BIGINT NOT NULL,
    school_id   BIGINT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_learners_parent FOREIGN KEY (parent_id) REFERENCES users(id),
    CONSTRAINT fk_learners_school FOREIGN KEY (school_id) REFERENCES schools(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_learners_parent ON learners(parent_id);
CREATE INDEX idx_learners_school ON learners(school_id);

-- OPERATORS
CREATE TABLE operators (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id               BIGINT NOT NULL,
    company_name          VARCHAR(150) NOT NULL,
    school_id             BIGINT NOT NULL,
    prdp_expiry           DATE,
    roadworthy_expiry     DATE,
    registration_expiry   DATE,
    active                TINYINT(1) NOT NULL DEFAULT 1,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_operators_user   FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_operators_school FOREIGN KEY (school_id) REFERENCES schools(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_operators_school ON operators(school_id);

-- VEHICLES
CREATE TABLE vehicles (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id          BIGINT NOT NULL,
    registration_number  VARCHAR(30) NOT NULL,
    make                 VARCHAR(50),
    model                VARCHAR(50),
    capacity             INT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicles_operator FOREIGN KEY (operator_id) REFERENCES operators(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_vehicles_operator ON vehicles(operator_id);

-- ROUTES
CREATE TABLE routes (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id  BIGINT NOT NULL,
    name         VARCHAR(150) NOT NULL,
    waypoints    TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_routes_operator FOREIGN KEY (operator_id) REFERENCES operators(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TRIPS
CREATE TABLE trips (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id     BIGINT NOT NULL,
    vehicle_id      BIGINT NOT NULL,
    route_id        BIGINT,
    route_name      VARCHAR(150),
    departure_time  DATETIME,
    arrival_time    DATETIME,
    status          ENUM('scheduled','on-route','delayed','deviated','completed')
                    NOT NULL DEFAULT 'scheduled',
    current_lat     DECIMAL(10,7),
    current_lng     DECIMAL(10,7),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trips_operator FOREIGN KEY (operator_id) REFERENCES operators(id),
    CONSTRAINT fk_trips_vehicle  FOREIGN KEY (vehicle_id)  REFERENCES vehicles(id),
    CONSTRAINT fk_trips_route    FOREIGN KEY (route_id)    REFERENCES routes(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_trips_operator ON trips(operator_id);
CREATE INDEX idx_trips_status   ON trips(status);

-- TRIP_LEARNERS
CREATE TABLE trip_learners (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trip_id         BIGINT NOT NULL,
    learner_id      BIGINT NOT NULL,
    picked_up       TINYINT(1) NOT NULL DEFAULT 0,
    dropped_off     TINYINT(1) NOT NULL DEFAULT 0,
    picked_up_at    DATETIME,
    dropped_off_at  DATETIME,
    CONSTRAINT fk_tl_trip    FOREIGN KEY (trip_id)    REFERENCES trips(id),
    CONSTRAINT fk_tl_learner FOREIGN KEY (learner_id) REFERENCES learners(id),
    UNIQUE KEY uq_trip_learner (trip_id, learner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_trip_learners_trip ON trip_learners(trip_id);

-- INCIDENTS
CREATE TABLE incidents (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    reported_by  BIGINT NOT NULL,
    trip_id      BIGINT,
    type         VARCHAR(50) NOT NULL,
    description  TEXT,
    status       ENUM('open','investigating','resolved') NOT NULL DEFAULT 'open',
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at  TIMESTAMP NULL,
    CONSTRAINT fk_incidents_user FOREIGN KEY (reported_by) REFERENCES users(id),
    CONSTRAINT fk_incidents_trip FOREIGN KEY (trip_id)     REFERENCES trips(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_incidents_status ON incidents(status);

-- MESSAGES
CREATE TABLE messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user   BIGINT NOT NULL,
    to_school   BIGINT NOT NULL,
    content     TEXT NOT NULL,
    reply_to    BIGINT,
    read_at     TIMESTAMP NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_user   FOREIGN KEY (from_user) REFERENCES users(id),
    CONSTRAINT fk_messages_school FOREIGN KEY (to_school) REFERENCES schools(id),
    CONSTRAINT fk_messages_reply  FOREIGN KEY (reply_to)  REFERENCES messages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_messages_school ON messages(to_school);

-- COMPLIANCE_DOCUMENTS
CREATE TABLE compliance_documents (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id  BIGINT NOT NULL,
    doc_type     ENUM('prdp','roadworthy','registration') NOT NULL,
    expiry_date  DATE NOT NULL,
    file_url     VARCHAR(255),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_compliance_operator FOREIGN KEY (operator_id) REFERENCES operators(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_compliance_operator ON compliance_documents(operator_id);

-- AUDIT_LOG
CREATE TABLE audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(100) NOT NULL,
    details     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_user    ON audit_log(user_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);
