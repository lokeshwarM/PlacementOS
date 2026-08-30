# Scaling Strategy

## Target

Support thousands of students.

## Design

- **Spring Boot**: Handles business requests, domain state, and transactions. Can scale horizontally.
- **Python/FastAPI Workers**: Handle heavy document processing (PDF, Excel, AI) and can scale independently of the core business logic.
- **Redis**: Provides asynchronous decoupling and robust queue management. It allows ingestion to acknowledge events immediately and hands off work to asynchronous consumers, isolating ingestion latency from processing.
- **PostgreSQL**: Hosted on Neon PostgreSQL, which handles underlying database scaling, connection pooling and storage. Remains the persistent system of record.
- Future horizontal scaling should not require changing business ownership between services.

## Future Scaling

- Multiple CDC inboxes.
- Horizontal workers.
- Monitoring.
- Retry queues.