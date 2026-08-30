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
Use Neon PostgreSQL as the primary PostgreSQL environment from the early development stage.
### Reason
It provides a persistent hosted PostgreSQL environment without requiring local database installation and keeps development close to the eventual deployed environment.
### Trade-off
Local development depends on network connectivity and an external hosted database.