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

Gmail Message Retrieval + MIME Normalization (Spring Boot)

↓

Durable PostgreSQL Persistence (`gmail_messages` & `attachments` metadata)

↓

`GMAIL_MESSAGE_RETRIEVED` Stream Event

↓

Python Processing Service:
  1. Deterministic Placement/Non-Placement Classification
  2. Multi-Role & Eligibility Extraction (Common vs Role-Specific)
  3. AI/LLM Fallback when Ambiguous
  4. Pydantic Output Schema Validation

↓

Internal Callback (`POST /api/v1/internal/extraction/result`)

↓

Spring Boot `PlacementIngestionService` (Validates & Idempotently Persists `PlacementDrive` + `PlacementRole`s in Neon PostgreSQL)

↓

Student-Level Eligibility Engine (`EligibilityEvaluationEngine` & `EligibilityService`):
  1. Combines Drive-Level Common Eligibility with Role-Specific Eligibility (Logical AND)
  2. Evaluates CGPA, Branch, Specialization, Batch, Degree, Standing Arrears, Gender
  3. Produces Explainable Criterion Results (PASS, FAIL, UNKNOWN, UNSUPPORTED)
  4. Idempotently Persists Per-Role Decision (ELIGIBLE, NOT_ELIGIBLE, REVIEW_REQUIRED) in `student_eligibility_results`

↓

Attachment Acquisition & Document Processing:
  1. `GmailAttachmentService` downloads attachment binary via Gmail API
  2. `AttachmentStorage` securely stores binary (isolated from PostgreSQL) and emits `ATTACHMENT_READY_FOR_PROCESSING`
  3. Python worker consumes event, downloads binary via protected internal endpoint, classifies document, and parses Excel/PDF/DOCX into structured candidate records
  4. Python posts structured candidate list to Spring Boot `POST /api/v1/internal/shortlists/result`
  5. `ShortlistMatchingService` matches candidates to students using strict identifier hierarchy (Reg No -> NeoPAT ID -> Unique Exact Name), flags conflicts/duplicates as `AMBIGUOUS`, and idempotently records `ShortlistEntry`

↓

Application State Reconciliation (`ApplicationStateReconciliationService`):
  1. Combines Eligibility Decisions, Shortlist Outcomes, and Current Application State
  2. Enforces non-regression: `APPLIED`, `SHORTLISTED`, `COMPLETED`, `REJECTED` never demote to `ELIGIBLE` / `NOT_ELIGIBLE`
  3. Never auto-applies on eligibility/shortlist (requires explicit student apply action)

↓

Notification Decision Engine (`NotificationDecisionService`):
  1. Deterministic evaluation for `ELIGIBILITY`, `SHORTLIST`, `DEADLINE`, `REMINDER`
  2. Computes structured `idempotency_key` (e.g. `eligibility:student:1:drive:10`, `reminder:task:5:slot:2`)
  3. Renders templates deterministically (`MessageTemplateService`)
  4. Atomically persists `Notification` and `NotificationOutbox` in PostgreSQL within the same business transaction

↓

Transactional Outbox & Delivery Worker (`NotificationOutboxService` & `NotificationDeliveryWorker`):
  1. Concurrency-safe claiming of due outbox records
  2. Dispatches to `WhatsAppNotificationProvider` (backed by testable `MockWhatsAppNotificationProvider`)
  3. Updates delivery status (`SENT`, `RETRYING` with exponential backoff, or `FAILED`)

↓

Reminder Lifecycle & Stop-on-Done (`ReminderService`):
  1. Recurring interval reminder evaluation
  2. Immediate cancellation of active `ReminderTask`s and pending outbox rows when student explicitly applies (`POST /api/v1/applications/{id}/apply`) or deadline passes

↓

Student Portal & End-to-End Workflow (Next.js App Router + Spring Boot Security):
  1. Student registers/authenticates via `POST /api/v1/auth/login` (JWT token issuance)
  2. Authenticated `Principal` resolves linked `Student` domain identity via `AuthenticatedStudentProvider`
  3. Onboarding & profile completeness lifecycle (`INCOMPLETE` → `COMPLETE` → `VERIFIED`)
  4. Dynamic dashboard displaying role-aware eligibility with explainable criteria breakdowns
  5. Explicit student application submission triggering reminder cancellation and outbox pruning
  6. Paginated notification center and reminder manager with interactive controls

## Distributed System Responsibility

- **Spring Boot**: Core business backend and sole source of truth for all business state (users, students, placements, placement roles, applications, shortlists, notifications, transactional outbox, reminders, normalized messages, attachment metadata). All persistence goes through Spring Boot.
- **Python/FastAPI**: Stateless processing service for email classification, regex/deterministic parsing, LLM fallback extraction, document attachment classification, and Excel/PDF/DOCX candidate extraction. Python does NOT directly mutate business tables.
- **PostgreSQL**: Persistent system of record (hosted on Neon PostgreSQL). Schema is managed by Flyway versioned migrations.
- **Redis Streams**: Asynchronous queue infrastructure used to decouple ingestion and processing. PostgreSQL remains the sole source of truth; Redis Streams provides durable at-least-once transport.
- **Next.js**: Student-facing portal consuming backend APIs and presenting role-aware eligibility, applications, notifications, and reminders without replicating backend business rules.

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
- Multi-role first class support
- Clean separation of common vs role-specific eligibility
- Never infer unstated eligibility restrictions
- Migration-based schema management
- Strict privacy: email bodies are sensitive data and excluded from operational logs

## Security Architecture

- **Application Security Boundary**: Spring Security protects all application endpoints. Controller logic is agnostic to the authentication mechanism to allow flexible identity provider substitution.
- **Identity Separation**: Student authentication (to use the application) is fundamentally separate from system source authentication (e.g., Gmail OAuth to read ingestion mailboxes).
- **Stateless Future**: The application is configured to eventually use stateless JWT/Bearer tokens. HTTP Sessions and CSRF are disabled.
- **Data Privacy**: Raw email bodies and HTML are stored durably for extraction but are strictly excluded from logs and diagnostics.