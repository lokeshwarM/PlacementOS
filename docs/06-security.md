# Security

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

## Authentication

Future versions will support secure student authentication.