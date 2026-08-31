# Interview Notes

## Example

Problem:
Duplicate Gmail notifications.

Solution:
Used Gmail Message-ID as an idempotency key stored in the `processed_emails` table with a UNIQUE constraint.

Trade-off:
Processed message metadata must be retained.

---

Question:
Why deterministic parsing first?

Answer:
Most CDC emails follow predictable formats. Regex is cheaper and faster than AI. AI is used as a fallback for unusual emails.

---

Question:
Why not use FastAPI for the entire backend?

Answer:
The application contains substantial business state, transactions, authentication, workflows, and relational persistence, while document and AI processing benefits from Python's ecosystem. Spring Boot owns the business domain; Python handles documents.

Trade-off:
More operational complexity because there are multiple services.

---

Question:
How are database credentials handled?

Answer:
Database configuration is externalized through a single `SPRING_DATASOURCE_URL` environment variable loaded from a local `backend/.env` file. Application code never contains credentials, and the secret file is excluded from Git via `backend/.gitignore`.

---

Question:
Why use Flyway instead of Hibernate auto schema creation?

Answer:
Flyway gives explicit, versioned, and reproducible database migrations. Hibernate is configured with `ddl-auto=validate` — it validates the schema on startup but never silently changes production structure. This gives full control over schema evolution and makes changes auditable.

---

Question:
How will you prevent the same CDC email from being processed multiple times?

Answer:
Every Gmail message has a globally unique Message-ID. Before any processing begins, we insert that ID into the `processed_emails` table. A UNIQUE constraint on `message_id` causes a duplicate-detection failure immediately, and we mark the attempt as DUPLICATE without proceeding.

---

Question:
Why have both `applications` and `placement_drives`?

Answer:
`placement_drives` represent the opportunity itself — company, deadline, eligibility criteria. `applications` represent an individual student's state for that opportunity (eligible, applied, shortlisted, etc.). Separating them avoids denormalization and allows one drive to have thousands of independent student states.

---

Question:
Why are Flyway and JPA both used?

Answer:
Flyway owns explicit, versioned database schema changes. Every structural change to the database is a migration file that can be replayed in any environment. JPA maps Java domain objects to that schema and provides the persistence abstractions (repositories, type-safe queries) that the Spring Boot business layer uses. Hibernate is configured with `ddl-auto=validate` — it validates the existing schema on startup but never silently modifies it. This combination gives you reproducible schema management and a clean Java programming model without letting the ORM surprise you in production.

---

Question:
Why separate student authentication from Gmail authentication?

Answer:
Student authentication establishes application identity and authorization. Gmail authentication grants the ingestion component access to an external mailbox. They serve different trust boundaries.

---

Question:
Why introduce Spring Security before implementing the identity provider?

Answer:
The controllers and services should be built behind a stable security boundary so authentication mechanisms can change without rewriting business logic.

---

Question:
Why must notification delivery be idempotent?

Answer:
Because background workers, retries, and network unreliability can cause the system to attempt sending the same notification multiple times. A composite unique constraint in the database ensures the student only ever receives that specific notification once, regardless of how many times the application layer tries to process it.

---

Question:
What is the difference between a public health check and exposing internal infrastructure details?

Answer:
A public health check (like `/actuator/health`) simply returns an UP/DOWN status so that load balancers and orchestrators know the application is alive. Exposing internal infrastructure details (like database versions, free disk space, or configurations) provides potential attackers with a reconnaissance map of your infrastructure. Health checks should be public; health details should be restricted.

---

Question:
Why is the current `reminder_tasks` constraint insufficient for the final engine, and why keep it for now?

Answer:
The current schema enforces uniqueness on `(student_id, placement_drive_id)`. However, the final product requires sending multiple reminders (e.g., 1 hour before deadline, 2 hours before deadline) for the same student and drive. We intentionally retain this limitation for now to avoid over-engineering the schema before the actual reminder scheduling engine is fully designed in a future milestone.

---

Question:
Why use Redis between Gmail ingestion and processing?

Answer:
The ingestion path should acknowledge the incoming event quickly and hand work to asynchronous consumers. This isolates ingestion latency from document/AI processing and allows workers to scale independently.

---

Question:
Why is Redis not the system of record?

Answer:
Redis is transport and temporary processing infrastructure. PostgreSQL remains the durable source of truth for business state.

---

### Gmail OAuth Authentication

**Why OAuth instead of storing Gmail passwords?**
OAuth provides delegated access without requiring the user to share their password. It is a secure standard that issues short-lived access tokens and revocable refresh tokens, which greatly reduces the risk if tokens are compromised.

**Why use gmail.readonly?**
The principle of least privilege. PlacementOS only needs to read incoming placement emails to parse them. It does not need to send emails, delete emails, or manage mailbox settings.

**Why is Gmail source identity separate from student identity?**
A CDC mailbox is an external data source integrated at the system level. Student identities (users of the platform) form a completely separate security domain. Mixing them would create authorization confusion.

**Why is OAuth state required?**
To protect the callback endpoint from Cross-Site Request Forgery (CSRF) attacks. By generating a cryptographically secure state and verifying it upon callback, we ensure the authorization flow was initiated by our application.

**Why must refresh tokens be treated as secrets?**
Refresh tokens are long-lived and can be repeatedly exchanged for new access tokens. If exposed, an attacker could gain persistent read access to the CDC mailbox. They must be stored securely (e.g., in Google Secret Manager or KMS-encrypted in the database) in production.

---

### Gmail Watch & Pub/Sub Ingestion

**Why use Google Cloud Pub/Sub push instead of pulling/polling the Gmail API?**
Polling requires constant periodic requests (e.g., every minute) regardless of whether a new email has arrived, wasting API quota and compute resources. Pub/Sub push provides event-driven, near real-time notification where Google actively calls our webhook the moment the mailbox changes.

**How do we authenticate that a webhook call actually came from Google?**
We use Google-signed JWT authentication. Every push delivery includes an `Authorization: Bearer <jwt>` header. Our endpoint verifies the JWT's cryptographic signature against Google's public certificates, and validates the issuer (`accounts.google.com`), the audience (our exact webhook URL), and the specific Google Service Account email configured for the push subscription.

**Why is the historyId stored as a String rather than a Number?**
Although `historyId` contains numeric characters, the Gmail API client library defines it as an opaque `String` for the `historyId` cursor. Storing it as a String (`VARCHAR(255)`) rather than a `BIGINT` ensures we respect the API contract, avoid truncation, and prevent issues if Google ever changes the format of the identifier.

**Does receiving a Pub/Sub notification mean an email has been fetched?**
No. The Pub/Sub notification only provides an opaque mailbox cursor (`historyId`). It signals that *something* changed. We must still use the Gmail History API (passing our last known `historyId`) to determine exactly what changed and fetch the actual email content. The webhook strictly tracks the cursor and handles idempotency; it does not process emails directly.

---

### Gmail Message Retrieval & MIME Normalization

**Question:**
Why separate `processed_emails` and `gmail_messages`?

**Answer:**
`processed_emails` manages ingestion and idempotency lifecycle states (`DISCOVERED`, `QUEUED`, `RETRIEVED`, `PROCESSED`, `FAILED`), while `gmail_messages` stores the normalized content and bodies acquired from Gmail. Separating them keeps the lightweight ingestion lifecycle independent from heavier message payloads.

**Question:**
Why normalize MIME instead of passing Gmail API objects downstream?

**Answer:**
A normalization layer creates a stable internal contract (`NormalizedGmailMessage`) and prevents downstream services (and eventual Python/FastAPI workers) from depending directly on Google's API representation or MIME serialization quirks.

**Question:**
How do you handle duplicate Redis events?

**Answer:**
The system uses PostgreSQL-backed durable idempotency (`uq_gmail_messages_source_message` unique constraint and checks) rather than relying on in-memory state. Repeated delivery of `GMAIL_MESSAGE_DISCOVERED` is recognized and treated as a harmless no-op.

**Question:**
Why not store attachment binaries in PostgreSQL?

**Answer:**
Large binary objects bloat database storage, impact backup performance, and increase memory footprint. The database stores attachment metadata and future storage references, while the binary files will be stored in an object store (e.g. S3 / GCS).

---

### Placement Classification & Structured Extraction

**Question:**
How do you handle a company with multiple roles?

**Answer:**
The placement drive is modeled separately from `PlacementRole` records. Each role can carry role-specific eligibility while shared criteria remains at the drive level.

**Question:**
How do you handle common and role-specific eligibility?

**Answer:**
Common eligibility applies to the drive (`placement_drives.eligibility_criteria`), while additional constraints are attached to the relevant role (`placement_roles.eligibility_criteria`). The later eligibility engine combines both.

**Question:**
Why not let the LLM directly create database records?

**Answer:**
LLM output is probabilistic. It first passes through a strict structured schema and validation boundary in Python before Spring Boot persists trusted business state in PostgreSQL.

**Question:**
Why deterministic extraction before AI?

**Answer:**
Predictable email structures can be parsed more cheaply and reproducibly. AI is reserved for ambiguous or irregular layouts.

**Question:**
What happens when eligibility is missing?

**Answer:**
The system preserves the field as unspecified/null rather than inventing a restriction.

---

### Student-Level Eligibility Engine

**Question:**
Why separate eligibility evaluation from placement extraction?

**Answer:**
Extraction determines what the company requires. Eligibility evaluation determines whether a particular student satisfies those requirements. They are different responsibilities.

**Question:**
Why have ELIGIBLE, NOT_ELIGIBLE, and REVIEW_REQUIRED?

**Answer:**
A missing student value is different from a failed criterion. The system should not incorrectly reject students simply because some required information is unavailable.

**Question:**
How do you combine common and role-specific eligibility?

**Answer:**
Common drive criteria are evaluated first and role-specific criteria are additional requirements. A role is eligible only when all applicable criteria are satisfied.

**Question:**
Why persist eligibility results?

**Answer:**
They will be reused by notifications, dashboards and application workflows, and persistence provides an auditable current decision.

---

### Attachment Processing & Shortlist Matching

**Question:**
Why not store attachment binaries in PostgreSQL?

**Answer:**
PostgreSQL stores durable business metadata well, while object/file storage is more appropriate for potentially large binary documents.

**Question:**
Why is document parsing in Python but student matching in Spring Boot?

**Answer:**
Python handles unstructured document processing, while Spring Boot owns trusted student identity and business state.

**Question:**
Why prioritize registration number and NeoPAT over names?

**Answer:**
Institutional identifiers are more reliable than names for identity matching.

**Question:**
How do you prevent incorrect name-based shortlist matches?

**Answer:**
Use exact normalized matching only when unique; ambiguous or weak matches become review-required rather than automatically linking to a student.