# Database Design

## Infrastructure
- **Primary Database**: PostgreSQL (Hosted on Neon)
- **Configuration**: Database credentials are strictly supplied through environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
- **Security**: Real secrets are never committed. A local `.env` file should be created in the `backend/` directory from `backend/.env.example`.
- **Ownership**: Spring Boot is the ONLY service that owns business persistence. The Python processing service does not directly own business state.

## students

| Field | Purpose |
|--------|---------|
| id | Primary Key |
| registration_number | Unique |
| neopat_id | Unique |
| name | Student Name |
| branch | Branch |
| batch | Graduation Batch |
| cgpa | Eligibility |
| phone | Notifications |

## placements

| Field | Purpose |
|--------|---------|
| id | Primary Key |
| gmail_message_id | Idempotency |
| company | Company |
| deadline | Deadline |
| branch_rule | Eligibility |
| cgpa_rule | Eligibility |
| batch_rule | Eligibility |
| email_received_at | Timestamp |

## attachments

| Field | Purpose |
|--------|---------|
| id | Primary Key |
| placement_id | Reference |
| filename | Original Name |
| type | Excel/PDF/DOCX |
| parsed | Status |

## notifications

| Field | Purpose |
|--------|---------|
| id | Primary Key |
| student_id | Student |
| placement_id | Placement |
| sent_at | Timestamp |
| status | Pending/Done |

## processed_messages

Stores processed Gmail Message IDs to prevent duplicate processing.