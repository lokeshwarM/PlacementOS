# High-Level Architecture

## Event Flow

CDC Email

↓

Gmail Watch API

↓

Redis Queue

↓

Email Parser

↓

Attachment Parser

↓

Placement Database

↓

Eligibility Engine

↓

Notification Queue

↓

Student Notification

## Principles

- Event-driven
- Idempotent
- Queue-based
- One parse per email