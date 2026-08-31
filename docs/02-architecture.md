# High-Level Architecture

## Event Flow

CDC Email

↓

Gmail Watch API (Google Cloud Pub/Sub)

↓

Pub/Sub Push Webhook (Spring Boot)

↓

Gmail History API Fetch (Spring Boot) — Discovers `messageId`

↓

Redis Streams (`placementos:events:stream` — Durable Event Transport)

↓

Redis Stream Consumer (Spring Boot)

↓

Gmail API `messages.get()` + MIME/Body Normalization

↓

Durable PostgreSQL Persistence (`gmail_messages` & `attachments` metadata)

↓

Email Parser (Python / Future) — reads from queue, sends extracted data to Spring Boot API

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

- **Spring Boot**: Core business backend and sole source of truth for all business state (students, placements, applications, shortlists, notifications, reminders, normalized messages, attachment metadata). All persistence goes through Spring Boot.
- **Python/FastAPI**: Stateless processing service for parsing PDFs, Excel, OCR, and AI-assisted extraction (deferred to future milestone). Python does NOT directly mutate business tables. All parsing results must pass through the Spring Boot API before becoming business state.
- **PostgreSQL**: Persistent system of record (hosted on Neon PostgreSQL). Schema is managed by Flyway versioned migrations.
- **Redis Streams**: Asynchronous queue infrastructure used to decouple ingestion (like Gmail discovery) from processing workers. PostgreSQL remains the sole source of truth; Redis Streams provides durable at-least-once transport.
- **Next.js**: Frontend interface.

## Persistence Rules

- Spring Boot is the only service with database write access to business tables.
- Python results must be submitted to Spring Boot endpoints and validated before storage.
- Schema changes require explicit Flyway migration files — Hibernate does not auto-create or auto-alter tables in any environment.

*(Note: Docker configuration is intentionally deferred until the deployment milestone)*

## Principles

- Event-driven
- Idempotent
- Durable Stream Queue-based
- One parse per email
- Migration-based schema management
- Strict privacy: email bodies are sensitive data and excluded from operational logs

## Security Architecture

- **Application Security Boundary**: Spring Security protects all application endpoints. Controller logic is agnostic to the authentication mechanism to allow flexible identity provider substitution.
- **Identity Separation**: Student authentication (to use the application) is fundamentally separate from system source authentication (e.g., Gmail OAuth to read ingestion mailboxes).
- **Stateless Future**: The application is configured to eventually use stateless JWT/Bearer tokens. HTTP Sessions and CSRF are disabled.
- **Data Privacy**: Raw email bodies and HTML are stored durably for extraction but are strictly excluded from logs and diagnostics.