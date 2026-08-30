# High-Level Architecture

## Event Flow

CDC Email

↓

Gmail Watch API

↓

Redis Queue

↓

Email Parser (Python)

↓

Attachment Parser (Python)

↓

Placement Database (Spring Boot + Neon PostgreSQL)

↓

Eligibility Engine (Spring Boot)

↓

Notification Queue (Redis)

↓

Student Notification

## Distributed System Responsibility

- **Spring Boot**: Core business backend and source of truth for business state (students, placements, rules).
- **Python/FastAPI**: Stateless processing service for parsing PDFs, Excel, OCR, and AI-assisted extraction.
- **PostgreSQL**: Persistent system of record (hosted on Neon PostgreSQL).
- **Redis**: Asynchronous queue infrastructure.
- **Next.js**: Frontend interface.

*(Note: Docker configuration is intentionally deferred until the deployment milestone)*

## Principles

- Event-driven
- Idempotent
- Queue-based
- One parse per email