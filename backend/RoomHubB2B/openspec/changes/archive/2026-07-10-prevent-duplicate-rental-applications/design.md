## Context

Booking creation currently validates tenant role, parses an hourly `[startAt, endAt)` interval, locks the target listing, creates a `bookings` row in `REQUESTED`, and writes one status-history row. Calendar conflicts are enforced later during landlord approval, so multiple `REQUESTED` rows from the same tenant for the same listing and day can be created and shown in participant inboxes.

The API already documents `409 Conflict` for booking operations, and `ApiExceptionHandler` maps `BookingConflictException` to `ProblemDetail`. The missing part is a creation-time duplicate rule plus a database guard for concurrent submissions.

## Goals / Non-Goals

**Goals:**

- Enforce one booking application per tenant organization, listing, and local `startAt` date.
- Return a deterministic `409 Conflict` ProblemDetail for duplicate `POST /api/bookings` attempts.
- Prevent race-condition duplicates with a PostgreSQL uniqueness guard.
- Keep the existing booking request/response DTOs and endpoint shape unchanged.

**Non-Goals:**

- Do not change overlap or availability behavior for different tenants.
- Do not introduce resubmission after cancellation/rejection; any existing booking on the same tenant/listing/start date remains a duplicate for this change.
- Do not change booking status transitions, approval-time calendar holding, inbox sorting, or OpenAPI paths.

## Decisions

1. Use `startAt.toLocalDate()` as the "same day" key.

   Rationale: booking requests already use local `LocalDateTime` values without tenant-specific time zones. Using the start date gives a stable key that matches the existing persistence model and avoids ambiguity for multi-hour or overnight intervals.

   Alternative considered: reject if any interval touches the same date. That would require a broader date-expansion rule for multi-day intervals and is outside the current MVP wording.

2. Check duplicates in `BookingService.create` before saving.

   Rationale: the service can return the same domain exception path used by the rest of booking workflow (`BookingConflictException` -> `409 ProblemDetail`) and can produce a clear message before attempting persistence.

   Alternative considered: rely only on a database uniqueness violation. That protects data, but it gives less intentional error handling and would require translating low-level `DataIntegrityViolationException` anyway.

3. Add a unique expression index on `bookings(tenant_organization_id, listing_id, (start_at::date))`.

   Rationale: PostgreSQL can enforce the invariant atomically during concurrent requests without adding a redundant date column to the JPA entity. Existing whole-hour and ordering constraints remain unchanged.

   Alternative considered: add a persisted `start_date` column. That can simplify repository queries, but it duplicates data and requires keeping column values synchronized with `start_at`.

4. Translate uniqueness races to `BookingConflictException`.

   Rationale: two concurrent duplicate requests can both pass the service pre-check before one insert fails. The create path should catch the unique-constraint failure, map only the named duplicate constraint to the duplicate booking message, and leave unrelated integrity failures unchanged.

## Risks / Trade-offs

- [Existing duplicate production rows] -> The migration will fail if duplicate tenant/listing/start-date rows already exist. Before deployment, run a duplicate detection query and resolve duplicates manually or with a one-off cleanup decision.
- [Expression index portability] -> This is PostgreSQL-specific, matching the project database. Tests should cover the migration through the existing PostgreSQL/Flyway setup rather than H2 assumptions.
- [Error message coupling] -> Tests should assert status and enough detail to prove duplicate handling, but avoid relying on unrelated ProblemDetail fields beyond the existing API pattern.
- [OpenAPI file churn] -> The path already includes `409`, so exporting may produce no semantic diff. Treat an unchanged export as acceptable evidence that the contract remains compatible.

## Migration Plan

1. Add a Flyway migration after the current latest version to create the unique index or constraint with a stable name, for example `uk_bookings_tenant_listing_start_date`.
2. Add a repository existence query for `tenantOrganizationId`, `listingId`, and a `[dateStart, dateEnd)` range on `startAt`.
3. Add service-level duplicate detection and race-condition translation in booking creation.
4. Add focused integration and schema tests, then run booking and OpenAPI contract tests.
5. Rollback is dropping the unique index and reverting service/repository logic; no request/response contract rollback is needed.
