# PlacementOS

PlacementOS is an event-driven placement workflow platform that automatically processes CDC placement emails, checks student eligibility, detects shortlist announcements, and sends personalized reminders.

## Core Principle

> Parse once. Personalize thousands of times.

## Architecture

PlacementOS uses a distributed architecture designed for scalability and clear separation of concerns:
- **Spring Boot**: Core business platform, managing student state, placements, and eligibility logic.
- **Python (FastAPI)**: Independent service dedicated to parsing Excel, PDF, and DOCX files, along with AI-assisted text extraction (deferred to future milestone).
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
- **Redis Streams Event Foundation**: Durable `GMAIL_MESSAGE_DISCOVERED` events published to `placementos:events:stream` with consumer groups.
- **Message Retrieval & MIME Normalization**: Recursively extracts plain text, HTML, and attachment metadata from Gmail messages without loading heavy binaries.
- **Durable Persistence**: `gmail_messages` and evolved `attachments` metadata records stored in PostgreSQL.