## ADDED Requirements

### Requirement: Frontend consumes backend booking inbox contract
The frontend SHALL consume the backend participant booking inbox endpoint as an authenticated list endpoint.

#### Scenario: Booking inbox is requested
- **WHEN** the frontend loads the booking inbox page
- **THEN** it SHALL send `GET /api/bookings` through the shared API client with bearer authentication

#### Scenario: Booking inbox response is consumed
- **WHEN** the backend returns booking inbox items
- **THEN** the frontend SHALL consume id, listingId, listingTitle, status, startAt, endAt, pricePerHour, totalPrice, confirmationDeadline, tenantOrganizationName, landlordOrganizationName, createdAt, and updatedAt

#### Scenario: Booking inbox status filter is used
- **WHEN** the frontend chooses to request a status-filtered inbox from the backend
- **THEN** it SHALL use the backend `status` query parameter with a known `BookingStatus` value

#### Scenario: Booking inbox request is unauthorized
- **WHEN** `GET /api/bookings` returns `401`
- **THEN** the frontend SHALL handle it through the existing auth/API behavior for protected booking requests
