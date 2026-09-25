# SafeRide SA

## Learner Transport Safety Management System

SafeRide SA is a learner transport safety management system designed to improve
the safety, monitoring and administration of learner transportation.

The system provides different features for users such as parents, operators,
school administrators and system administrators.

---

## Project Purpose

The purpose of SafeRide SA is to provide a centralised system for managing
learner transportation and improving communication, monitoring and safety.

The system aims to:

- Manage users and user roles
- Manage schools and school administrators
- Manage learner transport trips
- Monitor transport activities
- Record incidents and safety events
- Provide notifications and communication
- Maintain audit records of important system activities
- Improve the administration of learner transportation

---

## Main System Features

### User Authentication and Roles

The system supports role-based access for different users, including:

- Parent
- Operator
- School Administrator
- System Administrator

Each role is provided with features relevant to their responsibilities.

### System Administration

The System Administrator can:

- Manage user accounts
- Activate and suspend users
- Manage registered schools
- View school administrator information
- View audit logs
- Review system activity
- Monitor system alerts

### Transport Management

The system is designed to support:

- Trip scheduling
- Learner pickup and drop-off monitoring
- Trip status monitoring
- Route monitoring
- Trip history

### Safety and Incident Management

SafeRide SA supports safety-related functionality such as:

- Panic/emergency alerts
- Incident reporting
- Route deviation monitoring
- Driver and vehicle verification
- Safety notifications

### Communication

The system is designed to support communication between relevant users
through notifications and messaging.

---

## System Administrator Interface

The System Administrator section currently includes:

- Dashboard
- User Management
- School Management
- Audit Log Viewer

### Dashboard

The System Administrator dashboard provides an overview of:

- Total users
- Active users
- Registered schools
- Audit events
- Recent system activity
- System alerts

### User Management

The User Management page allows the System Administrator to:

- View registered users
- Search for users
- Filter users by role
- Filter users by status
- Add users
- Activate users
- Suspend users

### School Management

The School Management page allows the System Administrator to:

- View registered schools
- Search for schools
- Add schools
- View school details
- View administrator contact information

### Audit Logs

The Audit Log Viewer allows the System Administrator to:

- Search audit records
- Filter records by action
- View audit record details
- Review timestamped system activity
- Identify the user responsible for an action

Audit records are intended to become immutable and tamper-evident once
backend persistence is connected.

---

## Technologies

The project is being developed using technologies including:

- Java
- HTML
- CSS
- JavaScript
- JavaFX/Swing for the desktop application
- JDBC for database connectivity
- MySQL 8 / Oracle XE
- Git and GitHub

The current System Administrator interface contains frontend UI mockups and
client-side functionality. Backend and database integration will be connected
as the implementation progresses.

---

## Project Structure

```text
saferide-sa/
│
├── frontend/
│   ├── parent/
│   ├── operator/
│   ├── admin/
│   ├── system-admin/
│   │   ├── dashboard.html
│   │   ├── users.html
│   │   ├── schools.html
│   │   ├── audit-logs.html
│   │   └── js/
│   │       └── system-admin.js
│   │
│   └── css/
│       └── style.css
│
├── backend/
│   └── routes/
│
└── README.md
