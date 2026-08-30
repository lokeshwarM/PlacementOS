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