# API Design (v1)

Base URL: `/api/v1`

## OpenAPI/Swagger Status
OpenAPI documentation (Swagger UI) is **intentionally deferred** as per milestone requirements to avoid unnecessary dependencies at this stage. It will be added in a future milestone.

## Common Concepts
- **Validation**: All POST/PUT requests use standard Jakarta validation.
- **Error Handling**: A `@RestControllerAdvice` provides consistent error responses in JSON, returning `400 Bad Request`, `404 Not Found`, and `409 Conflict` appropriately.
- **DTOs**: Domain entities are NEVER returned directly from the API. DTOs are mapped for all requests and responses.

## Student APIs
- `GET /api/v1/students` - List all students
- `GET /api/v1/students/{id}` - Get student by ID
- `GET /api/v1/students/registration/{registrationNumber}` - Get student by registration number
- `GET /api/v1/students/neopat/{neopatId}` - Get student by NeoPAT ID
- `POST /api/v1/students` - Register a new student
- `PUT /api/v1/students/{id}` - Update a student

## Placement Drive APIs
- `GET /api/v1/placements` - List all placement drives
- `GET /api/v1/placements/{id}` - Get placement drive by ID (includes child `roles`)
- `GET /api/v1/placements/source-email/{sourceEmailId}` - Get by source email
- `POST /api/v1/placements` - Create a placement drive
- `PUT /api/v1/placements/{id}` - Update a placement drive

## Application APIs
- `POST /api/v1/applications` - Create an application (Admin / manual creation)
- `POST /api/v1/applications/{id}/apply` - Explicit student apply action (principal-derived student identity, records applied timestamp, cancels active reminders & pending outbox rows)
- `GET /api/v1/applications/my` - Get applications for the currently authenticated student
- `GET /api/v1/applications/{id}` - Get application by ID (enforces student ownership)
- `GET /api/v1/applications/lookup?studentId={id}&placementDriveId={id}` - Look up an application
- `POST /api/v1/applications/reconcile?driveId={id}&preferredRoleId={id}` - Idempotently reconcile student application state against eligibility results and shortlist outcomes

## Shortlist APIs
- `GET /api/v1/placements/{placementId}/shortlists` - Get shortlists for a drive
- `GET /api/v1/shortlists/registration/{registrationNumber}` - Get shortlists by registration number
- `GET /api/v1/shortlists/neopat/{neopatId}` - Get shortlists by NeoPAT ID
- `GET /api/v1/shortlists/name/{name}` - Get shortlists by name

## Notification APIs
- `GET /api/v1/notifications/my` - Get notification history for the currently authenticated student
- `GET /api/v1/notifications/student/{studentId}` - Get all notification states for a student (admin / verified ownership)
- `POST /api/v1/notifications/internal/outbox/process` - Internal trigger to drain transactional outbox records

## Reminder APIs
- `GET /api/v1/reminders/my` - Get active and historical reminders for the currently authenticated student
- `GET /api/v1/reminders/student/{studentId}` - Get all reminders for a student (admin / verified ownership)
- `POST /api/v1/reminders/{id}/stop` - Explicit action to cancel a reminder task and pending outbox rows
- `POST /api/v1/reminders/{id}/complete` - Mark a reminder as completed
- `POST /api/v1/reminders/process-due` - Internal trigger to evaluate and schedule due reminder tasks

## Eligibility APIs
- `POST /api/v1/eligibility/students/{studentId}/drives/{driveId}` - Evaluates and persists eligibility for all roles in a drive
- `GET /api/v1/eligibility/students/{studentId}/drives/{driveId}` - Retrieves persisted eligibility results for a drive
- `POST /api/v1/eligibility/students/{studentId}/roles/{roleId}` - Evaluates and persists eligibility for a specific role
- `GET /api/v1/eligibility/students/{studentId}/roles/{roleId}` - Retrieves persisted eligibility result for a specific role

## Processing & Extraction APIs (Internal)
- `POST /api/v1/internal/extraction/result` - Callback for Python processing service to submit structured extraction results to Spring Boot.
- `GET /api/v1/internal/messages/{messageId}` - Endpoint for Python worker to retrieve normalized email text and metadata.
- `GET /api/v1/internal/attachments/{id}/content` - Protected internal endpoint for Python worker to download attachment binary bytes (authenticated via `X-Internal-Service-Key`).
- `POST /api/v1/internal/shortlists/result` - Callback for Python worker to submit structured shortlist extraction candidates.
- `POST /api/v1/internal/attachments/{id}/process` - Development trigger to fetch and store attachment binary from Gmail API.
- `GET /api/v1/internal/shortlists/unresolved` - Endpoint to inspect ambiguous and review-required candidate records.
- `POST /api/internal/gmail/messages/retrieve` - Development trigger to fetch and persist raw Gmail message.
- `POST /api/internal/gmail/history/sync` - Development trigger for History API synchronization.