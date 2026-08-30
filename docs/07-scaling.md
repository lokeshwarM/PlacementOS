# Scaling Strategy

## Target

Support thousands of students.

## Design

- Parse emails once.
- Evaluate eligibility locally.
- Queue notifications.
- Use Redis workers.
- Prevent duplicate processing.

## Future Scaling

- Multiple CDC inboxes.
- Horizontal workers.
- Monitoring.
- Retry queues.