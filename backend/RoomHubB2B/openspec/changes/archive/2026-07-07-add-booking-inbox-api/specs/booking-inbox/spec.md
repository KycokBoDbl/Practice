## ADDED Requirements

### Requirement: Participant booking inbox
The system SHALL provide an authenticated read-only booking inbox endpoint for the current user's organization. The endpoint SHALL derive role and organization identity from the validated bearer token and SHALL NOT accept organization identity from request parameters or request body.

#### Scenario: Tenant reads outgoing booking requests
- **WHEN** an authenticated `TENANT` calls the booking inbox endpoint
- **THEN** the system responds `200 OK` with bookings whose `tenantOrganizationId` matches the tenant organization from the token
- **THEN** each item contains booking id, listing id, listing title, status, startAt, endAt, pricePerHour, totalPrice, confirmationDeadline, tenant organization name, landlord organization name, createdAt, and updatedAt

#### Scenario: Landlord reads incoming booking requests
- **WHEN** an authenticated `LANDLORD` calls the booking inbox endpoint
- **THEN** the system responds `200 OK` with bookings for listings whose owner organization matches the landlord organization from the token
- **THEN** bookings for listings owned by other organizations are absent from the response

#### Scenario: Unauthenticated inbox request is rejected
- **WHEN** a client calls the booking inbox endpoint without a valid bearer token
- **THEN** the system responds `401 Unauthorized` in the existing `ProblemDetail` format

### Requirement: Inbox ordering and filtering
The booking inbox endpoint SHALL return bookings in backend-defined recency order and SHALL support safe filters that do not weaken participant authorization.

#### Scenario: Inbox is ordered by recency
- **WHEN** multiple visible bookings exist for the current organization
- **THEN** the system returns the most recently changed or created bookings first using a stable tie-breaker

#### Scenario: Status filter limits visible results
- **WHEN** a participant calls the inbox endpoint with a supported `status` query parameter
- **THEN** the system returns only visible bookings with that status
- **THEN** bookings outside the participant organization remain absent regardless of the filter

#### Scenario: Unsupported filter is rejected
- **WHEN** a client sends an unsupported status or malformed query parameter
- **THEN** the system responds `400 Bad Request` in the existing `ProblemDetail` format

### Requirement: Inbox items link to existing booking detail
Every inbox item SHALL contain the booking id required to open the existing booking detail endpoint. The inbox endpoint SHALL NOT replace detail or history endpoints.

#### Scenario: Inbox item can be opened as detail
- **WHEN** a participant receives an item from the booking inbox response
- **THEN** the same participant can call `GET /api/bookings/{bookingId}` for that item and receive `200 OK`

#### Scenario: Inbox is read-only
- **WHEN** a participant reads the inbox
- **THEN** the system does not change booking status, status history, calendar blocks, or notification/read state
