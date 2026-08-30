# Interview Notes

## Example

Problem:
Duplicate Gmail notifications.

Solution:
Used Gmail Message-ID as an idempotency key.

Trade-off:
Requires storing processed message IDs.

---

Problem:
Why deterministic parsing first?

Solution:
Most CDC emails follow predictable formats.

Trade-off:
AI handles edge cases.

---

Problem:
Why not use FastAPI for the entire backend?

Decision:
Use Spring Boot for the core business platform and Python for processing/AI.

Reason:
The application contains substantial business state, transactions, authentication, workflows, and relational persistence, while document and AI processing benefits from Python's ecosystem.

Trade-off:
More operational complexity because there are multiple services.

---

Problem:
Why are environment variables used for database configuration instead of hardcoding credentials in properties?

Decision:
All Neon PostgreSQL configuration and secrets are supplied via `.env`.

Reason:
It is a fundamental security practice to keep secrets out of version control and the codebase. By requiring a local `.env` (ignored by Git) and tracking only `.env.example`, we prevent credential leakage and ensure that switching environments (e.g., local to production) doesn't require modifying source code.