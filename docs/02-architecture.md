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

Notification Queue (Redis — Future Milestone)

↓

Student Notification (Future Milestone)

## Distributed System Responsibility

- **Spring Boot**: Core business backend and sole source of truth for all business state (students, placements, placement roles, applications, shortlists, notifications, reminders, normalized messages, attachment metadata). All persistence goes through Spring Boot.
- **Python/FastAPI**: Stateless processing service for email classification, regex/deterministic parsing, LLM fallback extraction, and future PDF/Excel/OCR parsing. Python does NOT directly mutate business tables. All extraction results must pass through the Spring Boot API validation boundary before becoming trusted business state.
- **PostgreSQL**: Persistent system of record (hosted on Neon PostgreSQL). Schema is managed by Flyway versioned migrations.
- **Redis Streams**: Asynchronous queue infrastructure used to decouple ingestion and processing. PostgreSQL remains the sole source of truth; Redis Streams provides durable at-least-once transport.
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