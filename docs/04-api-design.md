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
- `GET /api/v1/placements/{id}` - Get placement drive by ID
- `GET /api/v1/placements/source-email/{sourceEmailId}` - Get by source email
- `POST /api/v1/placements` - Create a placement drive
- `PUT /api/v1/placements/{id}` - Update a placement drive

## Application APIs
- `POST /api/v1/applications` - Create an application (Apply to a drive)
- `GET /api/v1/applications/{id}` - Get application by ID
- `GET /api/v1/applications/lookup?studentId={id}&placementDriveId={id}` - Look up an application

## Shortlist APIs
- `GET /api/v1/placements/{placementId}/shortlists` - Get shortlists for a drive
- `GET /api/v1/shortlists/registration/{registrationNumber}` - Get shortlists by registration number
- `GET /api/v1/shortlists/neopat/{neopatId}` - Get shortlists by NeoPAT ID
- `GET /api/v1/shortlists/name/{name}` - Get shortlists by name

## Notification APIs
- `GET /api/v1/notifications/student/{studentId}` - Get all notification states for a student

## Reminder APIs
- `GET /api/v1/reminders/student/{studentId}` - Get all reminders for a student
- `POST /api/v1/reminders/{id}/complete` - Mark a reminder as completed

## Internal/Future APIs (Deferred)
- Processed Email APIs are internal and intentionally excluded from public endpoints.
- **Security**: The application is protected by Spring Security. `/api/v1/**` is temporarily permitted for development, but will be secured with JWT bearer tokens in the future.