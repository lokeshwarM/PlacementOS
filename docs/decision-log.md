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

---

## D-019
### Decision
Use Gmail OAuth 2.0 authorization-code flow for trusted CDC mailbox registration.
### Reason
PlacementOS needs delegated access to one or more trusted Gmail mailboxes without storing mailbox passwords.
### Trade-off
OAuth introduces credential lifecycle and token management requirements.

---

## D-020
### Decision
Treat Gmail source accounts as a separate integration/security domain.
### Reason
A CDC mailbox is an external data source, not a PlacementOS student identity.
### Trade-off
The system maintains a separate credential and lifecycle model for source accounts.

---

## D-021
### Decision
Encrypt Google OAuth refresh tokens at the application layer using AES-256-GCM before persisting them.
### Reason
OAuth refresh tokens are long-lived credentials. Encryption at rest reduces the impact of a database compromise while keeping credential ownership within the Gmail source integration boundary.
### Trade-off
Introduces key management and encryption/decryption complexity.

---

## D-022
### Decision
Persist refresh tokens as durable credentials while treating access tokens as transient credentials.
### Reason
Refresh tokens are required to obtain new access tokens after the original access token expires. Persisting short-lived access tokens provides little benefit and increases sensitive-data exposure.
### Trade-off
The application must perform token refresh when Gmail API access is needed.

---

## D-028
### Decision
Do not use `@Async` for Gmail history synchronization in the Pub/Sub push webhook handler.
### Reason
The Pub/Sub handler must not acknowledge the notification (HTTP 204) before the synchronization work has been safely accepted (written to DB and Redis). Synchronous execution guarantees that if processing fails or the server crashes, Pub/Sub will redeliver the notification.
### Trade-off
Large history synchronizations will block the webhook response, potentially causing Pub/Sub timeouts (default 10s).

---

## D-029
### Decision
Treat PostgreSQL idempotency checks and Redis event publication as completely separate, non-atomic systems.
### Reason
Distributed transactions (2PC) between PostgreSQL and Redis are brittle. Designing for at-least-once delivery with a durable idempotency guard in PostgreSQL makes duplicate Redis queueing harmless.
### Trade-off
A crash after Redis publication but before the final `QUEUED` DB state update means a message might be discovered and published twice, relying on downstream components to also be idempotent.

---

## D-030
### Decision
Introduce `DISCOVERED`, `QUEUED`, and `RETRIEVED` states in `processed_emails` instead of jumping straight to `PROCESSED`.
### Reason
Provides granular visibility into the ingestion lifecycle. `DISCOVERED` means the Gmail message ID was found but not yet successfully handed off to Redis. `QUEUED` confirms the handoff. `RETRIEVED` marks that message body and attachments have been safely persisted.
### Trade-off
Requires Flyway migrations to alter the `CHECK` constraint on `processing_status`.

---

## D-031
### Decision
The durable history cursor (`last_history_id`) must only advance after all discovered messages are safely queued.
### Reason
Advancing the cursor prematurely would cause the system to lose track of un-synced emails if a crash occurs during pagination or Redis publication.
### Trade-off
A failure midway through a multi-page sync will result in re-syncing the entire batch upon retry, highlighting the importance of idempotency.

---

## D-032
### Decision
Provide internal-only `POST /api/internal/gmail/history/sync` and `POST /api/internal/gmail/messages/retrieve` endpoints for manual triggering.
### Reason
Allows developers to manually simulate the end-to-end flow without needing to wait for a real Google Pub/Sub push notification.
### Trade-off
Must be strictly secured so that it does not become a public denial-of-service vector.

---

## D-033
### Decision
Persist normalized Gmail message data separately from processed email ingestion state.
### Reason
`processed_emails` tracks ingestion/idempotency lifecycle, while `gmail_messages` represents the acquired message content needed by downstream processing.
### Trade-off
Introduces another persistence model that must remain consistent with ingestion state.

---

## D-034
### Decision
Normalize Gmail MIME content before downstream processing rather than exposing Gmail API objects directly.
### Reason
A stable internal representation decouples the rest of PlacementOS from Google's API model and makes downstream processing deterministic.
### Trade-off
Requires explicit MIME traversal and decoding logic.

---

## D-035
### Decision
Store attachment metadata before implementing binary attachment storage.
### Reason
The initial retrieval milestone needs to know which files are present without introducing object storage complexity prematurely.
### Trade-off
Actual attachment binary processing is deferred.

---

## D-036
### Decision
Treat email content as sensitive data and exclude it from operational logs.
### Reason
Placement emails can contain personal or confidential information unrelated to the application's operational diagnostics.
### Trade-off
Debugging content-specific issues requires controlled database inspection or test fixtures rather than ordinary logs.

---

## D-037
### Decision
Represent multiple roles as first-class `PlacementRole` records rather than storing roles as a single text field.
### Reason
A placement drive can contain multiple positions and role-specific eligibility. A first-class role model allows each role to carry its own requirements without losing structure. `(placement_drive_id, role_title)` uniquely identifies a role within a single placement drive.
### Trade-off
Adds another relational entity and more persistence logic.

---

## D-038
### Decision
Separate drive-level/common eligibility from role-specific eligibility.
### Reason
Many placement emails apply one common eligibility rule to all positions while adding additional restrictions for specific roles.
### Trade-off
The future eligibility engine must combine common and role-specific criteria.

---

## D-039
### Decision
Use deterministic parsing before AI fallback wherever reliable.
### Reason
Structured placement emails often contain predictable fields and deterministic extraction is cheaper and more reproducible than LLM inference.
### Trade-off
Unusual email layouts require AI fallback.

---

## D-040
### Decision
Never infer unstated eligibility restrictions.
### Reason
Incorrect eligibility could cause students to miss opportunities or receive misleading notifications. Missing fields remain null/unspecified.
### Trade-off
Some emails will produce partially unspecified structured results requiring later review.

---

## D-041
### Decision
Validate LLM output against a strict structured schema before it can affect business state.
### Reason
LLM output is probabilistic and must not directly become trusted database state. Spring Boot validates extraction results before writing to PostgreSQL.
### Trade-off
Requires explicit schema validation and failure handling.

---

## D-042
### Decision
Keep placement classification separate from student eligibility evaluation.
### Reason
Classifying and extracting a placement drive is a source-document concern, while deciding whether a particular student qualifies is a business-rule concern.
### Trade-off
Adds an additional processing stage.

---

## D-043
### Decision
Evaluate eligibility as explicit criterion results followed by a final role decision.
### Reason
Students need an explainable result rather than a single opaque boolean.
### Trade-off
Requires a structured evaluation result.

---

## D-044
### Decision
Combine drive-level common eligibility with role-specific eligibility using logical AND.
### Reason
Role-specific constraints are additional requirements rather than replacements for common drive requirements.
### Trade-off
The evaluator must evaluate criteria at two levels.

---

## D-045
### Decision
Represent unknown student information separately from failed eligibility.
### Reason
Missing student data should not automatically imply ineligibility.
### Trade-off
Requires a REVIEW_REQUIRED outcome.

---

## D-046
### Decision
Do not automatically evaluate unsupported natural-language eligibility conditions.
### Reason
Unsafe inference can produce incorrect eligibility results.
### Trade-off
Some students will require manual review.

---

## D-047
### Decision
Persist the latest eligibility result per student-role combination.
### Reason
Eligibility results will later power notifications, dashboards, and application workflows.
### Trade-off
Results must be reevaluated when student or placement criteria change.

---

## D-048
### Decision
Version eligibility evaluator behavior using a simple evaluator version identifier.
### Reason
Future rule-engine changes may change outcomes and should remain traceable.
### Trade-off
Results must retain which evaluator version produced them.

---

## D-049
### Decision
Store attachment binaries through an abstraction rather than directly in PostgreSQL.
### Reason
Large document binaries should use storage designed for object/file persistence while PostgreSQL retains metadata and references.
### Trade-off
Introduces a storage layer and retention management.

---

## D-050
### Decision
Keep document parsing in Python while keeping student matching in Spring Boot.
### Reason
Python is responsible for unstructured document processing, while Spring Boot owns student identity and business state.
### Trade-off
Requires a structured processing result contract between services.

---

## D-051
### Decision
Prioritize registration number and NeoPAT ID over name matching.
### Reason
Strong institutional identifiers provide safer deterministic identity resolution than names.
### Trade-off
Documents containing only names require stricter ambiguity handling.

---

## D-052
### Decision
Never automatically shortlist a student using weak fuzzy name matching.
### Reason
False identity matches can associate the wrong student with a placement opportunity.
### Trade-off
Some name-only records require manual review.

---

## D-053
### Decision
Treat OCR-required documents as a distinct recoverable state.
### Reason
Scanned/image documents cannot be safely treated as successfully parsed text documents.
### Trade-off
Some documents require a future OCR capability.

---

## D-054
### Decision
Use structured document classifications rather than assuming every attachment is a shortlist.
### Reason
Placement emails commonly contain eligibility documents, job descriptions, recruitment instructions and shortlists in the same message.
### Trade-off
Requires document classification before shortlist extraction.