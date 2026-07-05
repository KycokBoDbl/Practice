## Purpose

Defines backend API and domain requirements discovered from the Spring Boot service, database migrations, and generated OpenAPI contract.

## Requirements

### Requirement: Backend supports legal entity authentication
The backend SHALL register one organization with one primary user and authenticate that user with signed bearer JWT access tokens.

#### Scenario: Legal entity registers
- **WHEN** valid `role`, `legalName`, `taxId`, `email`, and `password` are posted to `/api/auth/register`
- **THEN** the backend SHALL create an organization and user, normalize email, hash the password, enforce unique tax ID and email, and return profile data without password data

#### Scenario: User logs in
- **WHEN** valid credentials are posted to `/api/auth/login`
- **THEN** the backend SHALL return `accessToken`, `tokenType: Bearer`, and `expiresIn`

#### Scenario: Authenticated profile is requested
- **WHEN** `GET /api/auth/me` is called with a valid bearer token
- **THEN** the backend SHALL return the profile for the token subject

### Requirement: Backend publishes listing discovery and availability
The backend SHALL expose published commercial spaces and listing-local hourly availability as public endpoints.

#### Scenario: Published listings are requested
- **WHEN** `GET /api/listings` is called
- **THEN** the backend SHALL return published listing summaries with id, title, city, address, price, capacity, space type, image URL, and description

#### Scenario: Availability is requested
- **WHEN** `GET /api/listings/{listingId}/availability` receives hourly `from` and `to` query parameters
- **THEN** the backend SHALL return sorted busy intervals for the published listing in listing-local time without UTC offsets

### Requirement: Backend enforces authenticated booking workflow
The backend SHALL require bearer authentication and role-based authorization for booking workflow operations.

#### Scenario: Tenant creates booking request
- **WHEN** a `TENANT` posts `listingId`, `startAt`, and `endAt` to `/api/bookings`
- **THEN** the backend SHALL create a `REQUESTED` booking for a future whole-hour interval and calculate total price from listing price and duration

#### Scenario: Landlord approves booking
- **WHEN** the owning `LANDLORD` approves a `REQUESTED` booking
- **THEN** the backend SHALL atomically hold the listing interval, set status to `AWAITING_CONFIRMATION`, and set a confirmation deadline no later than the booking start

#### Scenario: Tenant confirms booking
- **WHEN** the owning `TENANT` confirms an `AWAITING_CONFIRMATION` booking before the deadline
- **THEN** the backend SHALL transition the booking to `CONFIRMED`

#### Scenario: Participant requests inaccessible booking
- **WHEN** a tenant or landlord requests a booking outside their organization boundary
- **THEN** the backend SHALL not expose the booking and SHALL respond as not found or forbidden according to the API contract

### Requirement: Backend maintains booking state history
The backend SHALL persist an append-only history entry for each booking status transition.

#### Scenario: Booking changes status
- **WHEN** a booking is created, approved, rejected, confirmed, cancelled, expired, started, or completed
- **THEN** the backend SHALL persist `fromStatus`, `toStatus`, `reason`, and `createdAt`

#### Scenario: Participant loads history
- **WHEN** an authorized participant calls `GET /api/bookings/{bookingId}/history`
- **THEN** the backend SHALL return history ordered by creation time and id

### Requirement: Backend keeps OpenAPI contract current
The backend SHALL publish the runtime OpenAPI document at `/api/openapi` and keep the exported `openapi/roomhub-b2b.openapi.json` synchronized with intentional API changes.

#### Scenario: Public API changes
- **WHEN** a controller mapping, DTO, validation annotation, response code, or security rule changes
- **THEN** the generated OpenAPI file SHALL be regenerated and committed with the backend change
