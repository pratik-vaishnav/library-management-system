# Software Requirements Specification (SRS)

## Project title

Assistive Offline-First Library Management System (EduShelf)

## 1. Introduction

### 1.1 Purpose
Define functional and non-functional requirements for an offline-capable, internet-synchronized library management system designed for remote operation by librarians, especially those with mobility or health limitations.

### 1.2 Scope
- Manage books, members, and transactions digitally.
- Operate offline at school premises.
- Synchronize with a central server when internet is available.
- Allow remote monitoring and control.
- Provide analytics, alerts, and assistive features.
- Optionally support live library monitoring.

### 1.3 Intended audience
- School management
- Librarians
- Developers and architects
- NGOs and CSR teams
- Government education departments

### 1.4 Definitions
- Offline-first: works without internet and syncs later
- Admin: librarian with full control
- Operator: staff handling daily issue/return
- Central server: cloud-hosted backend system

## 2. Overall description

### 2.1 Product perspective
- School desktop app (JavaFX) using SQLite
- Central server (Spring Boot) using PostgreSQL
- Admin client for remote access
- Optional streaming module for live view

### 2.2 User classes and roles
- Admin: full control, reports, settings, overrides
- Operator: issue/return, limited data entry
- Viewer: read-only access

### 2.3 Operating environment
- OS: Windows 10+
- Java: JDK 17+
- Local DB: SQLite
- Server DB: PostgreSQL
- Internet: scheduled or intermittent

## 3. Functional requirements

### 3.1 Authentication and authorization
- Secure login with username and password
- Role-based access control
- Token-based authentication for server access
- Session timeout and logout

### 3.2 Book management
- Add, update, disable books
- Search by title, author, ISBN
- Category filtering

### 3.3 Member management
- Add and edit students and teachers
- View borrowing history

### 3.4 Issue/return workflow
- Select member and book
- Auto-calculate due date and fines
- Store transactions offline
- Admin override support

### 3.5 Offline storage and sync
- Local SQLite with sync queue
- Manual and auto-sync
- Conflict handling and status indicators

### 3.6 Reports and analytics
- Issued and overdue reports
- Monthly statistics
- Inventory report
- Export to PDF and Excel

### 3.7 Alerts and notifications
- Overdue alerts
- Optional WhatsApp reminders
- Configurable templates

### 3.8 AI assistant (phase 2)
- Natural language queries
- Read-only insights initially

### 3.9 Audit logs
- Track issue/return actions
- Track overrides and settings changes

## 4. Non-functional requirements

- Performance: issue/return under 2 seconds
- Reliability: no data loss during outages
- Usability: simple UI, large fonts, keyboard-friendly
- Security: HTTPS, encrypted credentials, role-based access
- Scalability: multi-school support, multi-device
- Maintainability: modular architecture, clear APIs

## 5. Data model (high level)

- users
- books
- members
- transactions
- audit_logs
- sync_queue
- settings

## 6. Assumptions and constraints

Assumptions:
- At least one PC per school library
- Basic computer literacy

Constraints:
- Limited internet bandwidth
- Budget-sensitive environment
- Offline operation is mandatory

## 7. Success criteria

- Libraries operate without disruption during internet outages
- Librarians manage the system remotely
- Reduced overdue books
- Improved transparency and reporting
