# Database Design

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