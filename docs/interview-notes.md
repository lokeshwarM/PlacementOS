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