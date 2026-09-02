# Security

## Application Authentication Boundary

PlacementOS enforces a strict separation between **Application Identity** and **Source Identity**.

- **Application Identity (Student/User)**: Authentication to the PlacementOS platform APIs. This secures student profile data, placement records, applications, and reminders.
- **Source Identity (Gmail)**: Authentication required to ingest CDC placement emails via Gmail APIs. This is a system-level integration and does NOT grant student access.

**Decision (D-013)**: Gmail OAuth will NOT be used as the primary student authentication mechanism for the platform.

## Roles

The application defines two conceptual roles:

1. **STUDENT**: Can view and manage their own profile, applications, and notifications. Cannot access other students' data.
2. **ADMIN**: Can manage placement drives, view global processing states, and administer system settings.

*Note: Complex authorization enforcement is intentionally deferred to the Identity Provider integration milestone. The Security Foundation is in place to intercept unauthorized access.*

## API Security & Statelessness

- The application uses **Spring Security** as the protective boundary.
- **Stateless Design**: HTTP sessions are disabled (`SessionCreationPolicy.STATELESS`). The future implementation will use stateless tokens (e.g., JWT).
- **CSRF**: Disabled because the API is stateless and does not rely on browser cookie-based sessions for primary authentication.
- **CORS**: Wildcard `*` origins are avoided. Production configuration will restrict origins based on environment injection (e.g., frontend host).

## Temporary Development State

During the current development phase (before the Identity Provider is integrated):
- `/actuator/health` (with sensitive internal details restricted) and `/actuator/info` are permitted publicly.
- All other `/actuator/**` endpoints are strongly denied.
- Application APIs (`/api/v1/**`) are temporarily permitted to facilitate unblocked development of the frontend and processing components. This will be tightened to require `STUDENT` or `ADMIN` roles once token verification is implemented.

## Application Secrets

- **Environment Variables**: Real connection strings and credentials (such as Neon PostgreSQL passwords) must NEVER be hardcoded into the source code, `.yaml` properties, or documentation.
- **Git Safety**: The `.env` and `.env.*` files are explicitly ignored in `.gitignore` to prevent accidental credential leakage. Only `.env.example` with safe placeholders is tracked.

## Data Policy

Store only:
- Registration Number
- NeoPAT ID
- Name
- Branch
- Batch
- CGPA
- Phone Number

Do not process personal emails outside CDC.

## Transparency

Every notification should explain:
- Why the student is eligible.
- Which rule matched.
- Source email timestamp. 

## Gmail Source Identity

Gmail source authentication is separate from student authentication. The current scope is `gmail.readonly`. OAuth authorization uses the authorization-code flow. OAuth state protects the callback. 

**Credential Security**:
- OAuth **refresh tokens** are encrypted at rest at the application layer using **AES-256-GCM** before persistence.
- OAuth **access tokens** are treated as transient credentials. They are kept in memory to execute Gmail API calls and are NEVER stored durably in the database.
- Encryption keys must be externalized. The PostgreSQL database NEVER stores the encryption key. Production environments require robust secret management for the `GMAIL_TOKEN_ENCRYPTION_KEY`.
- Future key rotation is supported through a versioned ciphertext payload format (e.g. `v1:{iv}:{ciphertext}`).

## Pub/Sub Push Webhook

Google Cloud Pub/Sub pushes Gmail notifications to a dedicated webhook (`/api/internal/gmail/pubsub/push`).

**Push Authentication**:
- The webhook is authenticated via **Google-signed JWTs**.
- Every push delivery includes an `Authorization: Bearer <jwt>` header.
- PlacementOS validates the JWT signature against Google's public certificates.
- The JWT claims are strictly verified:
  - `iss` must be `https://accounts.google.com`.
  - `aud` must match the exact webhook URL.
  - `email` must match the specific Google Service Account configured for the push subscription.
  - `email_verified` must be true.
  - Expiry is enforced.
- **This is distinct from the OAuth token flow** and distinct from Spring Security role checks. The endpoint strictly rejects any push that fails JWT verification.

## Email Content Privacy & Sensitive Data

- **Content Protection**: Email bodies (`plain_text_body`, `html_body`) and attachment contents can contain sensitive student data, proprietary job descriptions, and salary info.
- **Operational Logging Rule**: Raw email bodies, HTML, and attachment binaries are **NEVER logged** to application logs or console output.
- **HTML Security**: Stored HTML bodies are treated as raw data only. They must never be rendered blindly in administrative or frontend views without sanitization to prevent Cross-Site Scripting (XSS).
- **Binary Data Boundary**: Large binary files are never stored in PostgreSQL or passed across Redis Streams. Only file metadata (filename, content type, attachmentId, size) is recorded in PostgreSQL.

## AI Processing & Service Isolation Boundary

- **No Direct Database Access**: The Python processing service has **zero direct access** to PostgreSQL business tables or database credentials.
- **No Direct Gmail Access**: Python has no access to Google OAuth refresh tokens or Gmail APIs. It only operates on normalized payloads provided by Spring Boot.
- **Strict Schema Validation**: Probabilistic AI/LLM outputs must pass strict Pydantic validation before being sent to Spring Boot, and Spring Boot strictly validates required fields (e.g., non-blank company name, confidence threshold) before writing to PostgreSQL.

## Student Eligibility Privacy & Access Control

- **Personal Profile Data**: Eligibility results reflect sensitive student data (CGPA, arrears, gender, academic standing).
- **Zero Logging Rule**: Student profiles, detailed criteria maps, and full evaluation JSON are excluded from ordinary application logs.
- **Endpoint Authorization**: In production, students may only view their own eligibility results; administrative oversight is restricted to ADMIN roles. (Temporarily accessible during development).

## Attachment Binary Security & Storage Isolation

- **Path Traversal Protection**: Storage references and keys are strictly validated and normalized to prevent directory traversal attacks (e.g. `../../`).
- **No Direct Execution**: Uploaded and downloaded attachment files are never executed and macro execution is disabled.
- **Internal Service Key Authentication**: The internal endpoint `GET /api/v1/internal/attachments/{id}/content` is protected by `X-Internal-Service-Key` and is never exposed publicly.
- **File Size Limits**: Ingestion enforces a strict maximum file size limit (25MB) to prevent Denial-of-Service.
- **Privacy in Logs**: Raw document contents, candidate tables, and personal identifiers from shortlist files are excluded from application logs. Only safe metadata (attachment ID, candidate count, match status) is logged.

## Principal-Derived Student Identity (Anti-Impersonation Boundary)

- **Zero Parameter Impersonation**: Student-facing endpoints (`POST /api/v1/applications/{id}/apply`, `GET /api/v1/applications/my`, `GET /api/v1/notifications/my`, `GET /api/v1/reminders/my`, `POST /api/v1/reminders/{id}/stop`) **never** trust a client-supplied query parameter (such as `?studentId=123`).
- **Principal Derivation**: The student's identity is resolved directly from the authenticated `SecurityContext` / `Principal` via `AuthenticatedStudentProvider`.
- **Cross-Student Access Prevention**: When an authenticated student interacts with an application, reminder, or notification, the service layer strictly verifies ownership (`app.getStudent().getId().equals(authenticatedStudent.getId())`) and throws `AccessDeniedException` (HTTP 403 Forbidden) if a student attempts to access or modify another student's record.
- **WhatsApp Isolation**: No live external WhatsApp credentials are baked into the codebase; all delivery operations in development and testing route through `MockWhatsAppNotificationProvider`.