# Engineering Decision Log

## D-001
### Decision
Use a shared CDC email source.
### Why
Every student receives the same placement email. Parsing once is dramatically cheaper.
### Trade-off
Requires trusted inboxes.

---

## D-002
### Decision
Use deterministic parsing first.
### Why
CDC emails are structured. Regex is cheaper than AI.
### Trade-off
AI fallback is needed for unusual emails.

---

## D-003
### Decision
Use Spring Boot for the core business platform and Python/FastAPI for document and AI processing.
### Reason
Spring Boot owns structured business workflows, persistence, transactions, authentication, and application state. Python is isolated for document processing and AI workloads where its ecosystem is better suited.
### Trade-off
This introduces inter-service communication and additional deployment complexity compared with a single backend.

---

## D-004
### Decision
Use Maven Wrapper instead of requiring a global Maven installation during development.
### Reason
The project should control its Maven version and remain reproducible across machines without requiring every developer to install Maven globally.
### Trade-off
The first build requires downloading the configured Maven distribution through the wrapper.

---

## D-005
### Decision
Defer Docker and Docker Compose until the application foundation and core functionality are working.
### Reason
Containerization is a deployment and environment-reproducibility concern. It is not required to establish the initial application architecture.
### Trade-off
Local development temporarily depends on host-installed runtime dependencies for PostgreSQL and Redis.

---

## D-006
### Decision
Use Neon PostgreSQL as the primary database environment from the early development stage.
### Reason
A hosted PostgreSQL environment provides persistent development data and keeps local development closer to the eventual deployment environment without requiring a local PostgreSQL installation.
### Trade-off
Development depends on network connectivity and an external hosted database.

---

## D-007
### Decision
Keep database credentials in `backend/.env` for local development and use a single `SPRING_DATASOURCE_URL` environment variable in application configuration.
### Reason
Secrets must not be hardcoded into source files or committed to the repository. Embedding credentials in the URL simplifies configuration to a single variable.
### Trade-off
Each development environment must provide its own configuration.

---

## D-008
### Decision
Use Flyway for versioned PostgreSQL schema migrations.
### Reason
The schema is expected to evolve as PlacementOS grows. Versioned migrations provide reproducible database changes across environments and avoid relying on automatic ORM schema mutation.
### Trade-off
Schema changes require explicit migration files and discipline.

---

## D-009
### Decision
Store a separate `processed_emails` table with a unique Gmail message ID.
### Reason
Multiple trusted CDC inboxes may receive the same email. A unique message identifier provides idempotent ingestion and prevents duplicate processing.
### Trade-off
Processed message metadata must be retained.

---

## D-010
### Decision
Use JPA entities as a persistence mapping layer over the Flyway-managed PostgreSQL schema.
### Reason
Flyway remains responsible for explicit schema evolution while JPA provides type-safe object-relational mapping and repository abstractions for the Spring Boot business layer.
### Trade-off
Entity definitions must remain synchronized with the versioned database schema. Flyway migrations, not Hibernate, remain the authoritative source of structural change.

---

## D-013
### Decision
Separate application user authentication from CDC Gmail source authentication.
### Reason
Students authenticate to PlacementOS as users, while Gmail access represents an external data source used for placement ingestion. These are different security domains and should not be coupled.
### Trade-off
Requires two separate authentication/integration flows.

---

## D-014
### Decision
Introduce Spring Security before production API exposure.
### Reason
The application contains student-specific placement, application, notification and reminder data that must not be accessible across users.
### Trade-off
Adds security configuration before the external identity provider is selected.

---

## D-015
### Decision
Enforce notification idempotency at the database level via a composite unique constraint.
### Reason
Worker retries, message queues, or network instability could result in the application attempting to process the same notification event multiple times. A database constraint ensures a student does not receive duplicate notifications for the same drive/channel.
### Trade-off
Adds a schema constraint that must be handled properly by the application layer.

---

## D-016
### Decision
Retain the current `reminder_tasks` uniqueness constraint (`student_id`, `placement_drive_id`) temporarily, despite its limitation for multiple scheduled reminders.
### Reason
The full reminder scheduling engine is deferred to a future milestone. Redesigning the schema before the reminder logic is fully understood adds premature complexity.
### Trade-off
Multiple reminders (e.g., 13:00, 14:00) for the same student/drive cannot be currently stored.

---

## D-017
### Decision
Use Redis as asynchronous infrastructure between event producers and processing workers.
### Reason
PlacementOS may need to process a single CDC event and fan out work across multiple processing tasks and thousands of student-specific operations. Asynchronous decoupling prevents a slow processing or notification operation from blocking ingestion.
### Trade-off
Introduces another infrastructure dependency and requires retry/idempotency handling.

---

## D-018
### Decision
Keep Redis messages small and pass references/identifiers for large payloads.
### Reason
Large documents, email bodies, and attachments should not be pushed directly through Redis unnecessarily.
### Trade-off
Consumers may need to retrieve the referenced data from persistent storage.