# Event Flow

1. **Gmail Watch Registration**: PlacementOS registers a watch with Google, receiving a mailbox cursor (`historyId`).
2. **CDC Email Arrives**: Google detects a change in the mailbox.
3. **Pub/Sub Push Delivery**: Google sends an authenticated, signed JWT push notification to the PlacementOS webhook.
4. **Webhook Authentication**: PlacementOS validates the Google-signed JWT (signature, issuer, audience, service account).
5. **Cursor Advancement**: If the incoming `historyId` is newer, the durable cursor is updated. (Emits boundary event `GMAIL_MAILBOX_CHANGED`).
6. **History API Synchronization**: PlacementOS fetches the new messages since the last `historyId` (to be implemented).
7. **Email Ingestion**: Unique message ID is checked to ignore duplicates.
8. **Event Transport**: Email metadata enters Redis Queue.
9. **Parsing**: Python worker consumes from Redis and extracts structured information.
10. **Storage**: Placement record is stored in PostgreSQL.
11. **Eligibility Evaluation**: Eligibility Engine evaluates students against placement criteria.
12. **Notification Dispatch**: Personalised alerts sent via Notification Queue.