# PlacementOS

PlacementOS is an event-driven placement workflow platform that automatically processes CDC placement emails, checks student eligibility, detects shortlist announcements, and sends personalized reminders.

## Core Principle

> Parse once. Personalize thousands of times.

## Architecture

PlacementOS uses a distributed architecture designed for scalability and clear separation of concerns:
- **Spring Boot**: Core business platform, managing student state, placements, and eligibility logic.
- **Python (FastAPI)**: Independent service dedicated to parsing Excel, PDF, and DOCX files, along with AI-assisted text extraction.
- **PostgreSQL**: System of record (Hosted on Neon PostgreSQL).
- **Redis**: Message broker and async task queue.
- **Next.js**: Frontend application.

*(Note: Docker and infrastructure containerization are intentionally deferred to a later milestone.)*

## Environment Setup

To run the application locally, you must provide your own Neon PostgreSQL credentials:
1. Copy `.env.example` to `.env`
2. Update the `.env` file with your `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.
3. The `.env` file is ignored by Git to ensure secrets are never committed.

## Status

Architecture/Setup Phase