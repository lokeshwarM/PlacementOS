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