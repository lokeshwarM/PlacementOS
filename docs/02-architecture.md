# High-Level Architecture

## Event Flow

CDC Email

↓

Gmail Watch API

↓

Redis Queue

↓

Email Parser (Python) — reads from queue, sends extracted data to Spring Boot API

↓

Spring Boot Attachment Parser trigger

↓

Placement Database (Spring Boot + Neon PostgreSQL — System of Record)

↓

Eligibility Engine (Spring Boot)

↓

Notification Queue (Redis)

↓

Student Notification

## Distributed System Responsibility

- **Spring Boot**: Core business backend and sole source of truth for all business state (students, placements, applications, shortlists, notifications, reminders). All persistence goes through Spring Boot.
- **Python/FastAPI**: Stateless processing service for parsing PDFs, Excel, OCR, and AI-assisted extraction. Python does NOT directly mutate business tables. All parsing results must pass through the Spring Boot API before becoming business state.
- **PostgreSQL**: Persistent system of record (hosted on Neon PostgreSQL). Schema is managed by Flyway versioned migrations.
- **Redis**: Asynchronous queue infrastructure (introduced in a later milestone).
- **Next.js**: Frontend interface.

## Persistence Rules

- Spring Boot is the only service with database write access to business tables.
- Python results must be submitted to Spring Boot endpoints and validated before storage.
- Schema changes require explicit Flyway migration files — Hibernate does not auto-create or auto-alter tables in any environment.

*(Note: Docker configuration is intentionally deferred until the deployment milestone)*

## Principles

- Event-driven
- Idempotent
- Queue-based
- One parse per email
- Migration-based schema management