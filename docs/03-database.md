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
students
  |
  +---- applications ---- placement_drives
  |
  +---- notifications ---- placement_drives
  |
  +---- reminder_tasks ---- placement_drives

placement_drives
  |
  +---- attachments
  |
  +---- shortlist_entries (also links to attachments)

processed_emails
  |
  +---- idempotency guard for Gmail ingestion
```

## Tables

### students
Stores VIT student profiles required for placement personalisation.

| Field               | Type          | Notes                       |
|---------------------|---------------|-----------------------------|
| id                  | BIGSERIAL     | Primary Key                 |
| registration_number | VARCHAR(20)   | UNIQUE, NOT NULL            |
| neopat_id           | VARCHAR(20)   | UNIQUE, nullable            |
| name                | VARCHAR(255)  | NOT NULL                    |
| branch              | VARCHAR(100)  | NOT NULL                    |
| batch               | INTEGER       | Graduation year, NOT NULL   |
| cgpa                | NUMERIC(4,2)  | 0.00–10.00 CHECK constraint |
| phone_number        | VARCHAR(20)   | Nullable                    |
| created_at          | TIMESTAMPTZ   | UTC, NOT NULL               |
| updated_at          | TIMESTAMPTZ   | UTC, NOT NULL               |

Indexes: `registration_number`, `neopat_id`, `name`

---

### placement_drives
Represents a unique placement opportunity extracted from a CDC communication.

| Field                | Type         | Notes                                |
|----------------------|--------------|--------------------------------------|
| id                   | BIGSERIAL    | Primary Key                          |
| company_name         | VARCHAR(255) | NOT NULL                             |
| title                | VARCHAR(500) | Nullable                             |
| description          | TEXT         | Nullable                             |
| received_at          | TIMESTAMPTZ  | When the email arrived               |
| application_deadline | TIMESTAMPTZ  | Nullable                             |
| source_email_id      | VARCHAR(255) | Gmail message ID of originating mail |
| eligibility_criteria | JSONB        | Extensible structured criteria       |
| status               | VARCHAR(50)  | OPEN / CLOSED / CANCELLED / COMPLETED|
| created_at           | TIMESTAMPTZ  | UTC, NOT NULL                        |
| updated_at           | TIMESTAMPTZ  | UTC, NOT NULL                        |

Indexes: `company_name`, `application_deadline`, `status`

---

### processed_emails
Idempotency guard for Gmail ingestion. Prevents the same email being processed more than once.

| Field              | Type         | Notes                                     |
|--------------------|--------------|-------------------------------------------|
| id                 | BIGSERIAL    | Primary Key                               |
| message_id         | VARCHAR(255) | **UNIQUE** Gmail Message-ID (globally unique) |
| thread_id          | VARCHAR(255) | Nullable                                  |
| source_identifier  | VARCHAR(255) | Nullable — inbox identifier               |
| received_at        | TIMESTAMPTZ  | Nullable                                  |
| processed_at       | TIMESTAMPTZ  | UTC, NOT NULL                             |
| processing_status  | VARCHAR(50)  | PENDING / PROCESSED / FAILED / DUPLICATE  |
| error_message      | TEXT         | Nullable — failure details                |

Indexes: `message_id`

---

### attachments
Tracks files from placement emails. Binary content is NOT stored in the database.

| Field              | Type         | Notes                                |
|--------------------|--------------|--------------------------------------|
| id                 | BIGSERIAL    | Primary Key                          |
| placement_drive_id | BIGINT       | FK → placement_drives                |
| filename           | VARCHAR(500) | NOT NULL                             |
| content_type       | VARCHAR(100) | MIME type, nullable                  |
| storage_reference  | TEXT         | Path or object-store reference       |
| parsed_status      | VARCHAR(50)  | PENDING / PARSED / FAILED / SKIPPED  |
| created_at         | TIMESTAMPTZ  | UTC, NOT NULL                        |

Indexes: `placement_drive_id`

---

### shortlist_entries
Represents a candidate found in a shortlist document. At least one of `registration_number`, `neopat_id`, or `candidate_name` is expected.

| Field               | Type          | Notes                               |
|---------------------|---------------|-------------------------------------|
| id                  | BIGSERIAL     | Primary Key                         |
| placement_drive_id  | BIGINT        | FK → placement_drives               |
| registration_number | VARCHAR(20)   | Nullable                            |
| neopat_id           | VARCHAR(20)   | Nullable                            |
| candidate_name      | VARCHAR(255)  | Nullable                            |
| source_attachment_id| BIGINT        | FK → attachments, nullable          |
| match_method        | VARCHAR(50)   | How the candidate was identified    |
| confidence          | NUMERIC(5,4)  | 0.0000–1.0000                       |
| created_at          | TIMESTAMPTZ   | UTC, NOT NULL                       |

Indexes: `placement_drive_id`, `registration_number`, `neopat_id`, `candidate_name`

---

### applications
Tracks an individual student's state for a placement drive.

| Field              | Type        | Notes                                           |
|--------------------|-------------|-------------------------------------------------|
| id                 | BIGSERIAL   | Primary Key                                     |
| student_id         | BIGINT      | FK → students                                   |
| placement_drive_id | BIGINT      | FK → placement_drives                           |
| status             | VARCHAR(50) | NOT_STARTED / ELIGIBLE / NOT_ELIGIBLE / APPLIED / SHORTLISTED / REJECTED / COMPLETED |
| applied_at         | TIMESTAMPTZ | Nullable                                        |
| created_at         | TIMESTAMPTZ | UTC, NOT NULL                                   |
| updated_at         | TIMESTAMPTZ | UTC, NOT NULL                                   |

Unique constraint: `(student_id, placement_drive_id)`
Indexes: `student_id`, `placement_drive_id`

---

### notifications
Tracks personalised notifications dispatched to students.

| Field              | Type        | Notes                                        |
|--------------------|-------------|----------------------------------------------|
| id                 | BIGSERIAL   | Primary Key                                  |
| student_id         | BIGINT      | FK → students                                |
| placement_drive_id | BIGINT      | FK → placement_drives                        |
| notification_type  | VARCHAR(50) | ELIGIBILITY / SHORTLIST / DEADLINE / REMINDER|
| channel            | VARCHAR(50) | WHATSAPP / TELEGRAM / EMAIL / IN_APP         |
| status             | VARCHAR(50) | PENDING / SENT / FAILED / SKIPPED            |
| sent_at            | TIMESTAMPTZ | Nullable                                     |
| created_at         | TIMESTAMPTZ | UTC, NOT NULL                                |

Indexes: `student_id`, `placement_drive_id`, `status`

---

### reminder_tasks
Represents a scheduled deadline reminder.

| Field              | Type        | Notes                                     |
|--------------------|-------------|-------------------------------------------|
| id                 | BIGSERIAL   | Primary Key                               |
| student_id         | BIGINT      | FK → students                             |
| placement_drive_id | BIGINT      | FK → placement_drives                     |
| scheduled_for      | TIMESTAMPTZ | NOT NULL                                  |
| status             | VARCHAR(50) | PENDING / COMPLETED / CANCELLED / FAILED  |
| completed_at       | TIMESTAMPTZ | Nullable                                  |
| created_at         | TIMESTAMPTZ | UTC, NOT NULL                             |
| updated_at         | TIMESTAMPTZ | UTC, NOT NULL                             |

Unique constraint: `(student_id, placement_drive_id)`