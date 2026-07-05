# frontend-booking-workflow Specification

## Purpose
TBD - created by archiving change align-frontend-with-backend-contract. Update Purpose after archive.
## Requirements
### Requirement: Booking API client matches backend contract
The frontend SHALL provide a typed booking API boundary for the existing authenticated backend booking endpoints.

#### Scenario: Booking request is created
- **WHEN** the frontend creates a booking request
- **THEN** it SHALL send `listingId`, `startAt`, and `endAt` to `POST /api/bookings`

#### Scenario: Participant booking is loaded
- **WHEN** the frontend loads a participant booking by id
- **THEN** it SHALL call `GET /api/bookings/{bookingId}` and consume booking id, listing id, status, interval, price, total price, and confirmation deadline

#### Scenario: Booking history is loaded
- **WHEN** the frontend loads booking history
- **THEN** it SHALL call `GET /api/bookings/{bookingId}/history` and consume ordered status transition entries

#### Scenario: Booking transition is submitted
- **WHEN** the frontend submits approve, reject, confirm, or cancel for a booking
- **THEN** it SHALL call the matching backend transition endpoint and consume the returned booking state

### Requirement: Tenant can create booking from selected calendar slot
The frontend SHALL allow an authenticated tenant to create a booking request from the selected booking calendar slot.

#### Scenario: Tenant confirms available selection
- **WHEN** an authenticated tenant selects an available date, start hour, and duration and confirms booking
- **THEN** the frontend SHALL create a booking request with an exclusive `endAt` calculated from start hour plus duration

#### Scenario: Booking request succeeds
- **WHEN** the backend returns a created booking
- **THEN** the frontend SHALL show the booking id, status, interval, total price, and next-step state to the user

#### Scenario: Authenticated landlord attempts tenant booking
- **WHEN** a user with role `LANDLORD` attempts to create a tenant booking
- **THEN** the frontend SHALL prevent submission where profile role is known and SHALL still handle backend `403` if returned

### Requirement: Booking conflicts are recoverable
The frontend SHALL treat backend booking conflicts as recoverable user-facing states.

#### Scenario: Backend returns booking conflict
- **WHEN** booking creation or transition returns `409 Conflict`
- **THEN** the frontend SHALL show a conflict message and refresh availability for the affected listing

#### Scenario: Backend returns validation error
- **WHEN** booking creation returns `400`
- **THEN** the frontend SHALL show a validation message without clearing the user's current listing context

#### Scenario: Backend returns unauthorized
- **WHEN** a booking request returns `401`
- **THEN** the frontend SHALL clear invalid auth state through the shared API/auth behavior and route the user to login

### Requirement: Participant can inspect booking status and history
The frontend SHALL provide a participant-facing booking detail experience for booking ids returned by the backend or opened directly.

#### Scenario: Participant opens booking detail
- **WHEN** an authenticated participant opens a booking detail route
- **THEN** the frontend SHALL load and render current booking status, interval, price, and confirmation deadline when present

#### Scenario: Participant opens booking history
- **WHEN** booking history is available
- **THEN** the frontend SHALL render transition entries in backend order with from status, to status, reason, and creation time

#### Scenario: Non-participant opens booking detail
- **WHEN** the backend returns `404` or `403` for a booking detail request
- **THEN** the frontend SHALL show an access-safe not-found or unavailable state without exposing unrelated booking data

### Requirement: Participant transition controls respect role and status
The frontend SHALL expose booking transition actions only when they match known participant role and current booking status.

#### Scenario: Landlord views requested booking
- **WHEN** an authenticated landlord views a `REQUESTED` booking for their listing
- **THEN** the frontend SHALL offer approve and reject actions

#### Scenario: Tenant views awaiting confirmation booking
- **WHEN** an authenticated tenant views an `AWAITING_CONFIRMATION` booking they own
- **THEN** the frontend SHALL offer confirm and cancel actions

#### Scenario: Booking is terminal
- **WHEN** a booking is `COMPLETED`, `REJECTED`, `CANCELLED`, or `EXPIRED`
- **THEN** the frontend SHALL show no mutating transition controls

