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