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

### Requirement: Backend supports landlord listing publication
The backend SHALL allow authenticated landlords to publish listings through the existing listings API without requiring a separate owner identifier in the request.

#### Scenario: Landlord publishes listing
- **WHEN** an authenticated `LANDLORD` sends valid listing data to `POST /api/listings`
- **THEN** the backend SHALL create a published listing owned by the landlord organization from the bearer token
- **AND** the response SHALL return the created listing using the standard listing response shape

#### Scenario: Tenant attempts to publish listing
- **WHEN** an authenticated `TENANT` sends a request to `POST /api/listings`
- **THEN** the backend SHALL reject the request with a forbidden response

#### Scenario: Listing response contains optional media fields
- **WHEN** a published listing has no description or image URL
- **THEN** the backend SHALL still return the listing successfully
- **AND** `description` and `imageUrl` SHALL be nullable in the listing response contract

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

### Requirement: Backend exposes participant booking inbox
The backend SHALL expose an authenticated participant booking inbox endpoint for tenants and landlords.

#### Scenario: Booking inbox is requested
- **WHEN** an authenticated participant calls `GET /api/bookings`
- **THEN** the backend SHALL return bookings visible to that participant organization

#### Scenario: Booking inbox response is returned
- **WHEN** the backend returns booking inbox items
- **THEN** each item SHALL include id, listingId, listingTitle, status, startAt, endAt, pricePerHour, totalPrice, confirmationDeadline, tenantOrganizationName, landlordOrganizationName, createdAt, and updatedAt

#### Scenario: Booking inbox status filter is used
- **WHEN** `GET /api/bookings` receives a `status` query parameter with a known booking status
- **THEN** the backend SHALL filter returned bookings by that status within the participant boundary

#### Scenario: Booking inbox request is unauthorized
- **WHEN** `GET /api/bookings` is called without valid bearer authentication
- **THEN** the backend SHALL reject the request as unauthorized

### Requirement: Backend keeps OpenAPI contract current
The backend SHALL publish the runtime OpenAPI document at `/api/openapi` and keep the exported `openapi/roomhub-b2b.openapi.json` synchronized with intentional API changes.

#### Scenario: Public API changes
- **WHEN** a controller mapping, DTO, validation annotation, response code, or security rule changes
- **THEN** the generated OpenAPI file SHALL be regenerated and committed with the backend change

### Requirement: Backend exposes public AI listing search for frontend consumption
The backend SHALL provide a public AI-assisted listing search endpoint that the frontend can call without changing existing listing DTO assumptions.

#### Scenario: AI listing search is requested
- **WHEN** the frontend posts `{ "prompt": "<text>" }` to `/api/listings/ai-search`
- **THEN** the backend SHALL return a JSON array of standard published listing response objects
- **AND** the frontend SHALL treat the response as `Listing[]`

#### Scenario: AI listing search prompt is invalid
- **WHEN** the AI listing search prompt is blank, malformed, or violates backend validation
- **THEN** the backend SHALL return a ProblemDetail-compatible `400` response
- **AND** the frontend SHALL parse it as a recoverable AI-search input error

#### Scenario: AI provider fails
- **WHEN** GigaChat OAuth, model request, timeout, or model response parsing fails
- **THEN** the backend SHALL return a ProblemDetail-compatible `502` response
- **AND** the frontend SHALL parse it as temporary AI-search unavailability

#### Scenario: AI listing search does not expose parsed filter metadata
- **WHEN** the backend returns AI search results
- **THEN** the frontend SHALL NOT depend on parsed filter, explanation, confidence, or ranking metadata that is not present in the API response
