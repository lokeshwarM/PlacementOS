# Event Flow

1. **Gmail Watch Registration**: PlacementOS registers a watch with Google, receiving a mailbox cursor (`historyId`).
2. **CDC Email Arrives**: Google detects a change in the mailbox.
3. **Pub/Sub Push Delivery**: Google sends an authenticated, signed JWT push notification to the PlacementOS webhook.
4. **Webhook Authentication**: PlacementOS validates the Google-signed JWT (signature, issuer, audience, service account).
5. **History API Synchronization**: PlacementOS fetches the new messages since the last `historyId`, acquiring discovery idempotency in `processed_emails`.
6. **Cursor Advancement**: The durable cursor is updated only after all discovered messages on all pages are safely queued.
7. **Durable Event Transport**: `GMAIL_MESSAGE_DISCOVERED` events are published to Redis Streams (`placementos:events:stream`).
8. **Redis Stream Consumer**: Background worker reads `GMAIL_MESSAGE_DISCOVERED` from consumer group `placementos-backend-group`.
9. **Message Retrieval & MIME Normalization**: Gmail `messages.get(format="full")` retrieves raw payload; MIME normalizer parses plain text, HTML, and attachment metadata.
10. **Durable PostgreSQL Persistence**: Saves `gmail_messages` and `attachments` metadata; updates `processed_emails` status to `RETRIEVED`.
11. **Parsing (Future)**: Python worker consumes from queue and extracts structured placement information.
12. **Storage (Future)**: Placement record is stored in PostgreSQL.
13. **Eligibility Evaluation (Future)**: Eligibility Engine evaluates students against placement criteria.
14. **Notification Dispatch (Future)**: Personalised alerts sent via Notification Queue.