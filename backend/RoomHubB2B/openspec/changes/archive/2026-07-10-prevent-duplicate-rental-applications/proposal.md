## Why

Tenants can currently submit multiple booking requests for the same listing on the same calendar day, which creates duplicate work for landlords and makes the booking inbox noisy. The MVP needs a clear conflict rule so one tenant organization can have at most one application per listing per day.

## What Changes

- Reject `POST /api/bookings` when the authenticated tenant organization already has a booking request for the same `listingId` whose requested interval starts on the same local date.
- Return `409 Conflict` with the existing `application/problem+json` error format for the duplicate attempt.
- Preserve existing successful booking creation behavior, role checks, listing visibility checks, interval validation, price snapshotting, booking status history, and inbox discovery.
- Add database-level protection so concurrent duplicate submissions cannot create more than one matching booking.

Example duplicate request:

```http
POST /api/bookings
Content-Type: application/json
Authorization: Bearer <tenant-token>

{
  "listingId": 42,
  "startAt": "2030-01-10T14:00",
  "endAt": "2030-01-10T16:00"
}
```

If the same tenant already has a booking for listing `42` with `startAt` on `2030-01-10`, the response is:

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Tenant already has a booking request for this listing on this date",
  "instance": "/api/bookings"
}
```

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `booking-workflow`: Booking creation gains a per-tenant, per-listing, per-start-date uniqueness rule and a duplicate-attempt conflict scenario.

## Impact

- Booking creation API behavior changes only for duplicate submissions; endpoint, request DTO, response DTO, and authentication model remain unchanged.
- PostgreSQL schema needs an additional uniqueness guard on `bookings` using tenant organization, listing, and the local date derived from `start_at`.
- Backend implementation touches booking repository/service error handling and should reuse `BookingConflictException` so `ApiExceptionHandler` and `ApiProblemWriter` continue producing `ProblemDetail`.
- Tests need focused service/integration coverage for duplicate rejection and a schema/concurrency check for database enforcement.
- Frontend contract risk is low because `POST /api/bookings` already documents `409 Conflict`; frontend should handle this as a user-facing duplicate application error.
