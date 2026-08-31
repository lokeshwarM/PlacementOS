# Event Flow

1. **Gmail Watch Registration**: PlacementOS registers a watch with Google, receiving a mailbox cursor (`historyId`).
2. **CDC Email Arrives**: Google detects a change in the mailbox.
3. **Pub/Sub Push Delivery**: Google sends an authenticated, signed JWT push notification to the PlacementOS webhook.
4. **Webhook Authentication**: PlacementOS validates the Google-signed JWT (signature, issuer, audience, service account).
5. **History API Synchronization**: PlacementOS fetches the new messages since the last `historyId`, acquiring discovery idempotency in `processed_emails`.
6. **Cursor Advancement**: The durable cursor is updated only after all discovered messages on all pages are safely queued.
7. **Durable Event Transport**: `GMAIL_MESSAGE_DISCOVERED` events are published to Redis Streams (`placementos:events:stream`).
8. **Message Retrieval & MIME Normalization**: Spring Boot consumer reads `GMAIL_MESSAGE_DISCOVERED`, fetches full payload via Gmail API, extracts plain text, HTML, and attachment metadata, and saves to `gmail_messages` and `attachments`.
9. **Message Retrieved Event**: Spring Boot publishes `GMAIL_MESSAGE_RETRIEVED` to Redis Streams.
10. **Classification & Extraction**: Python worker consumes `GMAIL_MESSAGE_RETRIEVED`, classifies placement vs. non-placement, deterministically parses company, multi-roles, common vs. role-specific eligibility, deadlines, and dates (with LLM fallback for ambiguous formats), and validates against strict Pydantic schema.
11. **Extraction Ingestion Callback**: Python worker submits structured extraction result to Spring Boot `POST /api/v1/internal/extraction/result`.
12. **Durable Placement Persistence**: Spring Boot `PlacementIngestionService` validates extraction payload, idempotently creates/updates `PlacementDrive` and child `PlacementRole` records in PostgreSQL, and updates `processed_emails` status to `EXTRACTED` (or `NON_PLACEMENT`).
13. **Eligibility Evaluation (Future Milestone)**: Eligibility Engine evaluates students against common and role-specific placement criteria.
14. **Notification Dispatch (Future Milestone)**: Personalised alerts sent via Notification Queue.