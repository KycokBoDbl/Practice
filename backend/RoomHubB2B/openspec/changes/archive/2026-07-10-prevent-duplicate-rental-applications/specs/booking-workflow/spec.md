## ADDED Requirements

### Requirement: Duplicate booking applications are rejected
The system SHALL allow an authenticated `TENANT` organization to have at most one booking application for the same published listing and the same local `startAt` calendar date. A duplicate `POST /api/bookings` attempt MUST fail with `409 Conflict` using the existing `application/problem+json` error format and MUST NOT create a booking or booking status history row.

#### Scenario: Tenant repeats an application for the same listing and day
- **WHEN** an authenticated `TENANT` already has a booking whose `listingId` matches the request and whose `startAt` is on the same local date as the new request
- **THEN** `POST /api/bookings` responds with `409 Conflict`
- **THEN** the response uses `application/problem+json` and explains that the tenant already has a booking request for this listing on this date
- **THEN** no additional booking or booking status history row is created

#### Scenario: Same tenant applies to the same listing on another day
- **WHEN** an authenticated `TENANT` already has a booking for a listing on one local `startAt` date
- **THEN** the same tenant can create a booking application for the same listing when the new `startAt` is on a different local date and all other booking creation rules pass

#### Scenario: Different tenant applies to the same listing and day
- **WHEN** one tenant organization already has a booking for a listing on a local `startAt` date
- **THEN** another tenant organization can create a booking application for the same listing and local `startAt` date when all other booking creation rules pass

#### Scenario: Concurrent duplicate submissions are serialized
- **WHEN** two concurrent `POST /api/bookings` requests from the same tenant target the same listing and local `startAt` date
- **THEN** at most one booking is committed
- **THEN** the losing request responds with `409 Conflict` instead of creating a duplicate row
