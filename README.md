# PlacementOS

PlacementOS is an event-driven placement workflow platform that automatically processes CDC placement emails, checks student eligibility, detects shortlist announcements, and sends personalized reminders.

## Core Principle

> Parse once. Personalize thousands of times.

## Architecture

PlacementOS uses a distributed architecture designed for scalability and clear separation of concerns:
- **Spring Boot**: Core business platform, managing student state, placements, placement roles, and eligibility logic.
- **Python (FastAPI)**: Stateless processing service dedicated to email classification, deterministic text extraction, LLM fallback parsing, and future PDF/Excel/OCR file parsing.
- **PostgreSQL**: System of record (Hosted on Neon PostgreSQL).
- **Redis Streams**: Message broker and durable async stream queue.
- **Next.js**: Frontend application.

*(Note: Docker and infrastructure containerization are intentionally deferred to a later milestone.)*

## Environment Setup

To run the application locally, you must provide your own Neon PostgreSQL and Redis credentials:
1. Navigate to the `backend/` directory.
2. Copy `backend/.env.example` to `backend/.env`.
3. Update the `.env` file with your `SPRING_DATASOURCE_URL` (this single URL should contain your username, password, and SSL parameters).
4. Update the `.env` file with your `SPRING_DATA_REDIS_URL` (Upstash Redis URL or local Redis URL).
5. The `backend/.env` file is ignored by Git to ensure secrets are never committed.

## Database

- **Provider**: Neon PostgreSQL (hosted)
- **Schema management**: Flyway — versioned SQL migrations in `backend/src/main/resources/db/migration/`
- **Credentials**: Environment-based via `SPRING_DATASOURCE_URL` only — never hardcoded
- **Persistence owner**: Spring Boot exclusively — Python never writes directly to business tables
- **Hibernate**: Configured with `ddl-auto=validate` — validates schema only, never mutates it

## Pipeline Features

- **Gmail Integration**: OAuth 2.0 (`gmail.readonly`) with AES-256-GCM encrypted refresh token storage.
- **Pub/Sub Webhook**: Google-signed JWT authenticated push endpoint for mailbox synchronization.
- **History Synchronization**: Synchronous Gmail History API pagination and discovery idempotency.
- **Redis Streams Event Foundation**: Durable `GMAIL_MESSAGE_DISCOVERED` and `GMAIL_MESSAGE_RETRIEVED` events published to `placementos:events:stream` with consumer groups.
- **Message Retrieval & MIME Normalization**: Recursively extracts plain text, HTML, and attachment metadata from Gmail messages without loading heavy binaries.
- **Durable Message Persistence**: `gmail_messages` and evolved `attachments` metadata records stored in PostgreSQL.
- **Placement Email Classification**: Deterministic classification engine identifying placement vs. non-placement emails with confidence scoring.
- **Structured Multi-Role Extraction**: Extracts company, drive title, multiple roles (`PlacementRole`), common drive-level eligibility, role-specific eligibility, deadlines, and important dates with strict schema validation.
- **Idempotent Ingestion**: Spring Boot `PlacementIngestionService` validates extraction results and idempotently persists placement opportunities in PostgreSQL.
- **Student-Level Eligibility Engine**: Evaluates student profiles against drive common criteria and role-specific criteria, providing explainable criterion results (`PASS`, `FAIL`, `UNKNOWN`, `UNSUPPORTED`), and idempotently persists per-role decisions (`ELIGIBLE`, `NOT_ELIGIBLE`, `REVIEW_REQUIRED`) in PostgreSQL.
- **Attachment Acquisition & Shortlist Matching**: Downloads attachment binaries from Gmail API, stores files securely via `AttachmentStorage` abstraction, parses Excel, PDF, and DOCX documents in Python, and deterministically matches shortlist candidates to registered students using strict identifier hierarchy (Registration Number -> NeoPAT ID -> Unique Exact Name).
- **Application State Reconciliation**: Reconciles student applications across eligibility decisions and shortlist matches while preserving authoritative forward states (`APPLIED`, `SHORTLISTED`, `COMPLETED`, `REJECTED`).
- **Notification Decision Engine**: Deterministically generates notification intent with structured `idempotency_key`s (`eligibility:...`, `shortlist:...`, `deadline:...`, `reminder:...`) avoiding memory scans or duplicate dispatches.
- **Transactional Outbox & Delivery Worker**: Atomically persists notification records and outbox entries in PostgreSQL within the business transaction, drained asynchronously with concurrency-safe locking and exponential backoff.
- **Telegram Notification Delivery & Bot Integration**: Active student notification channel via `TelegramNotificationProvider` over HTTPS, featuring cryptographic one-time deep-link account linking (`/start <token>`), secret-authenticated webhook update handling with idempotency, and interactive `/done` or `DONE:<appId>` application state transitions that automatically halt reminder lifecycles. (Pluggable `WhatsAppNotificationProvider` retained for legacy/multi-channel extensibility).
- **Reminder State & Stop-on-Done**: Recurring interval reminder engine that automatically stops active tasks and cancels pending outbox reminders when a student explicitly marks `APPLIED` (via portal or Telegram `/done`) or the deadline passes.
- **Principal-Derived Student Identity**: Complete anti-impersonation boundary deriving student identity from authenticated `SecurityContext` / `Principal` across all student-facing endpoints (zero trust of client-supplied `?studentId=...`).