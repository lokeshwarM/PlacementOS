# Event Flow

1. Gmail Watch receives a new CDC email.
2. Gmail Message ID is checked.
3. Duplicate emails are ignored.
4. Email metadata enters Redis Queue (Event Envelope with ID).
5. Parser worker consumes from Redis and extracts structured information.
6. Attachments are parsed.
7. Placement record is stored.
8. Eligibility Engine evaluates every student.
9. Notification Queue sends personalized alerts.
10. Reminder Scheduler manages follow-ups.