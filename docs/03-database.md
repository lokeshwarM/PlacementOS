# Database Design

## Infrastructure
- **Primary Database**: PostgreSQL (Hosted on Neon)
- **Configuration**: Database credentials are supplied through the single environment variable `SPRING_DATASOURCE_URL`, which embeds username, password, and SSL parameters.
- **Migrations**: Managed by **Flyway**. Schema changes are versioned SQL files. Hibernate validates the schema on startup but never mutates it.
- **Security**: Real secrets are never committed. Create `backend/.env` from `backend/.env.example`.
- **Ownership**: Spring Boot is the ONLY service that owns business persistence. The Python processing service never directly mutates business tables.

## Timestamp Convention
All timestamps are stored as `TIMESTAMPTZ` (UTC). No local-timezone text values are stored.

## Migration Strategy
- Location: `backend/src/main/resources/db/migration/`
- Naming: `V{version}__{description}.sql`
- Applied automatically by Flyway on startup.
- Progress is tracked in the `flyway_schema_history` table managed by Flyway.
- Never edit an already-applied migration file.

## Table Relationships

```
users (Authentication Identity & Profile Lifecycle)
  |
  +---- (1:1 optional) ---- students (Academic & Placement Domain Identity)
                              |
                              +---- student_eligibility_results ---- placement_roles & placement_drives
                              |
                              +---- applications ---- placement_drives & placement_roles
                              |
                              +---- notifications ---- placement_drives & placement_roles
                              |
                              +---- reminder_tasks ---- placement_drives & placement_roles

gmail_sources
  |
  +---- gmail_messages
          |
          +---- attachments (can link to gmail_messages or placement_drives)

placement_drives
  |
  +---- placement_roles (first-class role support with role-specific eligibility)
  |       |
  |       +---- student_eligibility_results (persisted evaluation per role)
  |
  +---- attachments
  |
  +---- shortlist_entries (also links to attachments)

processed_emails
  |
  +---- idempotency guard for Gmail ingestion
```

## Tables

### users
Stores authentication accounts, hashed credentials, roles, profile completion lifecycle states, and 1-to-1 linkage to domain students.

| Field          | Type         | Notes                                                        |
|----------------|--------------|--------------------------------------------------------------|
| id             | BIGSERIAL    | Primary Key                                                  |
| email          | VARCHAR(255) | UNIQUE, NOT NULL                                             |
| password_hash  | VARCHAR(255) | BCrypt hashed password, NOT NULL                             |
| role           | VARCHAR(50)  | STUDENT / ADMIN, NOT NULL                                    |
| student_id     | BIGINT       | UNIQUE, FK → students(id) ON DELETE SET NULL, nullable       |
| profile_status | VARCHAR(50)  | INCOMPLETE / COMPLETE / VERIFIED, NOT NULL                   |
| created_at     | TIMESTAMPTZ  | UTC, NOT NULL                                                |
| updated_at     | TIMESTAMPTZ  | UTC, NOT NULL                                                |

Indexes: `email`, `student_id`, `role`, `profile_status`

---

### gmail_sources
Stores registered Gmail accounts (CDC inboxes) and their access credentials for email ingestion.

| Field              | Type         | Notes                                      |
|--------------------|--------------|--------------------------------------------|
| id                 | BIGSERIAL    | Primary Key                                |
| email_address      | VARCHAR(255) | UNIQUE, NOT NULL                           |
| provider           | VARCHAR(50)  | NOT NULL                                   |
| credential         | TEXT         | AES-256-GCM encrypted refresh token        |
| status             | VARCHAR(50)  | ACTIVE / ERROR / HISTORY_STALE             |
| last_history_id    | VARCHAR(255) | Opaque cursor string for History API       |
| watch_expiration   | TIMESTAMPTZ  | When the current Pub/Sub watch expires     |
| watch_status       | VARCHAR(50)  | NONE / ACTIVE / EXPIRED                    |
| created_at         | TIMESTAMPTZ  | UTC, NOT NULL                              |
| updated_at         | TIMESTAMPTZ  | UTC, NOT NULL                              |

Indexes: `email_address`

---

### gmail_messages
Stores acquired and normalized raw Gmail message payloads and metadata.

| Field               | Type         | Notes                                             |
|---------------------|--------------|---------------------------------------------------|
| id                  | BIGSERIAL    | Primary Key                                       |
| message_id          | VARCHAR(255) | Gmail Message-ID, NOT NULL                        |
| gmail_source_id     | BIGINT       | FK → gmail_sources(id), NOT NULL                  |
| thread_id           | VARCHAR(255) | Nullable                                          |
| subject             | VARCHAR(500) | Extracted Subject header                          |
| sender              | VARCHAR(255) | Extracted From header                             |
| recipients          | TEXT         | Extracted To and Cc headers                       |
| plain_text_body     | TEXT         | Extracted plain text body                         |
| html_body           | TEXT         | Extracted HTML body                               |
| snippet             | TEXT         | Short message snippet                             |
| gmail_internal_date | TIMESTAMPTZ  | Internal Gmail timestamp                          |
| retrieval_status    | VARCHAR(50)  | RETRIEVED / FAILED                                |
| retrieved_at        | TIMESTAMPTZ  | UTC, NOT NULL                                     |
| created_at          | TIMESTAMPTZ  | UTC, NOT NULL                                     |
| updated_at          | TIMESTAMPTZ  | UTC, NOT NULL                                     |

Unique constraint: `(gmail_source_id, message_id)`
Indexes: `message_id`, `gmail_source_id`, `thread_id`

---

### students
Stores VIT student profiles required for placement personalisation.

| Field               | Type          | Notes                                                 |
|---------------------|---------------|-------------------------------------------------------|
| id                  | BIGSERIAL     | Primary Key                                           |
| registration_number | VARCHAR(20)   | UNIQUE, NOT NULL                                      |
| neopat_id           | VARCHAR(20)   | UNIQUE, nullable                                      |
| name                | VARCHAR(255)  | NOT NULL                                              |
| branch              | VARCHAR(100)  | NOT NULL                                              |
| batch               | INTEGER       | Graduation year, NOT NULL                             |
| cgpa                | NUMERIC(4,2)  | 0.00–10.00 CHECK constraint                           |
| phone_number        | VARCHAR(20)   | Nullable                                              |
| degree              | VARCHAR(50)   | E.g. B.Tech, M.Tech, MCA, nullable                    |
| specialization      | VARCHAR(100)  | E.g. Artificial Intelligence, nullable                 |
| standing_arrears    | INTEGER       | Nullable (NULL = unknown, 0 = 0 arrears, >0 = arrears)|
| gender              | VARCHAR(20)   | E.g. MALE, FEMALE, OTHER, nullable                    |
| created_at          | TIMESTAMPTZ   | UTC, NOT NULL                                         |
| updated_at          | TIMESTAMPTZ   | UTC, NOT NULL                                         |

Indexes: `registration_number`, `neopat_id`, `name`

---

### placement_drives
Represents a unique placement opportunity extracted from a CDC communication.

| Field                | Type         | Notes                                             |
|----------------------|--------------|---------------------------------------------------|
| id                   | BIGSERIAL    | Primary Key                                       |
| company_name         | VARCHAR(255) | NOT NULL                                          |
| title                | VARCHAR(500) | Nullable                                          |
| description          | TEXT         | Nullable                                          |
| received_at          | TIMESTAMPTZ  | When the email arrived                            |
| application_deadline | TIMESTAMPTZ  | Nullable                                          |
| source_email_id      | VARCHAR(255) | **UNIQUE** Gmail message ID of originating mail   |
| eligibility_criteria | JSONB        | Common drive-level eligibility criteria           |
| status               | VARCHAR(50)  | OPEN / CLOSED / CANCELLED / COMPLETED             |
| created_at           | TIMESTAMPTZ  | UTC, NOT NULL                                     |
| updated_at           | TIMESTAMPTZ  | UTC, NOT NULL                                     |

Unique constraint: `(source_email_id)`
Indexes: `company_name`, `application_deadline`, `status`, `source_email_id`

---

### placement_roles
Represents an individual job role/position within a placement drive.

| Field                | Type         | Notes                                             |
|----------------------|--------------|---------------------------------------------------|
| id                   | BIGSERIAL    | Primary Key                                       |
| placement_drive_id   | BIGINT       | FK → placement_drives(id) ON DELETE CASCADE       |
| role_title           | VARCHAR(255) | Role title, NOT NULL                              |
| role_description     | TEXT         | Nullable                                          |
| role_order           | INTEGER      | Ordering number within the drive, NOT NULL        |
| eligibility_criteria | JSONB        | Role-specific eligibility criteria (if specified) |
| created_at           | TIMESTAMPTZ  | UTC, NOT NULL                                     |
| updated_at           | TIMESTAMPTZ  | UTC, NOT NULL                                     |

Unique constraint: `(placement_drive_id, role_title)`
Indexes: `placement_drive_id`

---

### student_eligibility_results
Persisted, explainable eligibility evaluation decisions for a student on a specific placement role.

| Field                | Type         | Notes                                                              |
|----------------------|--------------|--------------------------------------------------------------------|
| id                   | BIGSERIAL    | Primary Key                                                        |
| student_id           | BIGINT       | FK → students(id) ON DELETE CASCADE, NOT NULL                      |
| placement_drive_id   | BIGINT       | FK → placement_drives(id) ON DELETE CASCADE, NOT NULL              |
| placement_role_id    | BIGINT       | FK → placement_roles(id) ON DELETE CASCADE, NOT NULL               |
| decision             | VARCHAR(50)  | ELIGIBLE / NOT_ELIGIBLE / REVIEW_REQUIRED, NOT NULL                 |
| criteria_results     | JSONB        | Detailed structured list of evaluated criteria & reasons, NOT NULL |
| evaluator_version    | VARCHAR(50)  | E.g. eligibility-v1, NOT NULL                                      |
| evaluated_at         | TIMESTAMPTZ  | When evaluation was executed, NOT NULL                             |
| created_at           | TIMESTAMPTZ  | UTC, NOT NULL                                                      |
| updated_at           | TIMESTAMPTZ  | UTC, NOT NULL                                                      |

Unique constraint: `(student_id, placement_role_id)`
Indexes: `student_id`, `placement_drive_id`, `placement_role_id`

---

### processed_emails
Idempotency guard for Gmail ingestion. Prevents the same email being processed more than once.

| Field              | Type         | Notes                                                              |
|--------------------|--------------|--------------------------------------------------------------------|
| id                 | BIGSERIAL    | Primary Key                                                        |
| message_id         | VARCHAR(255) | **UNIQUE** Gmail Message-ID (globally unique)                      |
| thread_id          | VARCHAR(255) | Nullable                                                           |
| source_identifier  | VARCHAR(255) | Nullable — inbox identifier                                        |
| received_at        | TIMESTAMPTZ  | Nullable                                                           |
| processed_at       | TIMESTAMPTZ  | UTC, NOT NULL                                                      |
| processing_status  | VARCHAR(50)  | PENDING / DISCOVERED / QUEUED / RETRIEVED / EXTRACTED / NON_PLACEMENT / PROCESSED / FAILED / DUPLICATE |
| error_message      | TEXT         | Nullable — failure details                                         |

Indexes: `message_id`

---

### attachments
Tracks files from placement emails. Binary content is NOT stored in the database; `storage_reference` points to secure file/object storage managed via `AttachmentStorage`.

| Field                    | Type         | Notes                                                              |
|--------------------------|--------------|--------------------------------------------------------------------|
| id                       | BIGSERIAL    | Primary Key                                                        |
| placement_drive_id       | BIGINT       | FK → placement_drives, nullable                                    |
| gmail_message_record_id  | BIGINT       | FK → gmail_messages(id), nullable                                  |
| attachment_id            | VARCHAR(255) | Gmail API attachmentId, nullable                                   |
| byte_size                | BIGINT       | File size in bytes, nullable                                       |
| filename                 | VARCHAR(500) | NOT NULL                                                           |
| content_type             | VARCHAR(100) | MIME type, nullable                                                |
| storage_reference        | TEXT         | Storage reference key                                              |
| sha256_checksum          | VARCHAR(64)  | SHA-256 integrity checksum, nullable                               |
| parsed_status            | VARCHAR(50)  | PENDING / DOWNLOADED / PROCESSING / EXTRACTED / PARSED / FAILED / OCR_REQUIRED / REVIEW_REQUIRED / SKIPPED |
| created_at               | TIMESTAMPTZ  | UTC, NOT NULL                                                      |

Check constraint: `placement_drive_id IS NOT NULL OR gmail_message_record_id IS NOT NULL`
Indexes: `placement_drive_id`, `gmail_message_record_id`, `sha256_checksum`

---

### shortlist_entries
Represents a candidate found in a shortlist document and deterministically matched to a registered student.

| Field                | Type          | Notes                                                 |
|----------------------|---------------|-------------------------------------------------------|
| id                   | BIGSERIAL     | Primary Key                                           |
| placement_drive_id   | BIGINT        | FK → placement_drives, NOT NULL                       |
| student_id           | BIGINT        | FK → students, nullable (matched student)             |
| placement_role_id    | BIGINT        | FK → placement_roles, nullable (role association)     |
| registration_number  | VARCHAR(20)   | Cleaned registration number, nullable                 |
| neopat_id            | VARCHAR(20)   | Cleaned NeoPAT ID, nullable                           |
| candidate_name       | VARCHAR(255)  | Candidate name, nullable                              |
| source_attachment_id | BIGINT        | FK → attachments, nullable                            |
| match_status         | VARCHAR(50)   | UNMATCHED / MATCHED / AMBIGUOUS / REVIEW_REQUIRED     |
| match_method         | VARCHAR(50)   | REGISTRATION_NUMBER / NEOPAT_ID / EXACT_NORMALIZED_NAME |
| match_reason         | TEXT          | Explainable match reason or conflict explanation      |
| raw_evidence         | TEXT          | Document snippet evidence                             |
| confidence           | NUMERIC(5,4)  | 0.0000–1.0000                                         |
| created_at           | TIMESTAMPTZ   | UTC, NOT NULL                                         |
| updated_at           | TIMESTAMPTZ   | UTC, NOT NULL                                         |

Indexes: `placement_drive_id`, `student_id`, `placement_role_id`, `match_status`, `source_attachment_id`, `registration_number`, `neopat_id`, `candidate_name`

---

### applications
Tracks an individual student's state for a placement drive or specific role.

| Field              | Type        | Notes                                           |
|--------------------|-------------|-------------------------------------------------|
| id                 | BIGSERIAL   | Primary Key                                     |
| student_id         | BIGINT      | FK → students, NOT NULL                         |
| placement_drive_id | BIGINT      | FK → placement_drives, NOT NULL                 |
| placement_role_id  | BIGINT      | FK → placement_roles, nullable (role context)   |
| status             | VARCHAR(50) | NOT_STARTED / ELIGIBLE / NOT_ELIGIBLE / APPLIED / SHORTLISTED / REJECTED / COMPLETED |
| applied_at         | TIMESTAMPTZ | Nullable (timestamp when student marked applied)|
| created_at         | TIMESTAMPTZ | UTC, NOT NULL                                   |
| updated_at         | TIMESTAMPTZ | UTC, NOT NULL                                   |

Unique constraint: `(student_id, placement_drive_id)`
Indexes: `student_id`, `placement_drive_id`, `placement_role_id`

---

### notifications
Tracks personalised notifications dispatched to students with durable idempotency.

| Field              | Type         | Notes                                        |
|--------------------|--------------|----------------------------------------------|
| id                 | BIGSERIAL    | Primary Key                                  |
| idempotency_key    | VARCHAR(255) | **UNIQUE**, NOT NULL (deterministic key)     |
| student_id         | BIGINT       | FK → students, NOT NULL                      |
| placement_drive_id | BIGINT       | FK → placement_drives, NOT NULL              |
| placement_role_id  | BIGINT       | FK → placement_roles, nullable               |
| notification_type  | VARCHAR(50)  | ELIGIBILITY / SHORTLIST / DEADLINE / REMINDER|
| channel            | VARCHAR(50)  | WHATSAPP / TELEGRAM / EMAIL / IN_APP         |
| status             | VARCHAR(50)  | PENDING / SENT / FAILED / SKIPPED            |
| message_payload    | TEXT         | Rendered message body, nullable              |
| sent_at            | TIMESTAMPTZ  | Nullable                                     |
| created_at         | TIMESTAMPTZ  | UTC, NOT NULL                                |

Unique constraint: `(idempotency_key)`
Indexes: `idempotency_key`, `student_id`, `placement_drive_id`, `placement_role_id`, `(student_id, placement_drive_id)`

---

### notification_outbox
Transactional outbox table ensuring atomic consistency between business events and asynchronous message dispatches.

| Field              | Type         | Notes                                        |
|--------------------|--------------|----------------------------------------------|
| id                 | BIGSERIAL    | Primary Key                                  |
| notification_id    | BIGINT       | FK → notifications(id) ON DELETE CASCADE     |
| idempotency_key    | VARCHAR(255) | **UNIQUE**, NOT NULL                         |
| status             | VARCHAR(50)  | PENDING / PROCESSING / SENT / FAILED / RETRYING / CANCELLED |
| channel            | VARCHAR(50)  | WHATSAPP / TELEGRAM / EMAIL / IN_APP         |
| recipient          | VARCHAR(255) | Destination phone number / email             |
| payload            | TEXT         | Serialized message text                      |
| attempt_count      | INTEGER      | Default 0                                    |
| max_attempts       | INTEGER      | Default 3                                    |
| available_at       | TIMESTAMPTZ  | Next eligible processing time                |
| processed_at       | TIMESTAMPTZ  | Nullable                                     |
| last_error         | TEXT         | Nullable — failure details                   |
| created_at         | TIMESTAMPTZ  | UTC, NOT NULL                                |
| updated_at         | TIMESTAMPTZ  | UTC, NOT NULL                                |

Indexes: `(status, available_at)`, `notification_id`, `idempotency_key`

---

### reminder_tasks
Represents scheduled recurring deadline reminders with stop-on-apply lifecycle management.

| Field              | Type         | Notes                                     |
|--------------------|--------------|-------------------------------------------|
| id                 | BIGSERIAL    | Primary Key                               |
| student_id         | BIGINT       | FK → students, NOT NULL                   |
| placement_drive_id | BIGINT       | FK → placement_drives, NOT NULL           |
| placement_role_id  | BIGINT       | FK → placement_roles, nullable            |
| scheduled_for      | TIMESTAMPTZ  | Next occurrence timestamp, NOT NULL       |
| interval_minutes   | INTEGER      | Recurrence interval (default 60 mins)     |
| max_reminders      | INTEGER      | Maximum reminder iterations (default 5)   |
| reminders_sent     | INTEGER      | Count of sent reminder iterations         |
| cancel_reason      | VARCHAR(255) | STUDENT_APPLIED / DEADLINE_PASSED / etc.  |
| last_reminder_at   | TIMESTAMPTZ  | Nullable                                  |
| status             | VARCHAR(50)  | PENDING / COMPLETED / CANCELLED / FAILED  |
| completed_at       | TIMESTAMPTZ  | Nullable                                  |
| created_at         | TIMESTAMPTZ  | UTC, NOT NULL                             |
| updated_at         | TIMESTAMPTZ  | UTC, NOT NULL                             |

Unique constraint: `(student_id, placement_drive_id)`
Indexes: `(status, scheduled_for)`, `placement_role_id`