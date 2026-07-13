## ADDED Requirements

### Requirement: Header exposes booking inbox navigation
The frontend SHALL expose a Header navigation entry to the booking inbox for authenticated users.

#### Scenario: Authenticated user sees bookings navigation
- **WHEN** an authenticated user sees the Header
- **THEN** the Header SHALL include a navigation entry to the booking inbox route

#### Scenario: Guest does not see protected bookings navigation
- **WHEN** a guest sees the Header
- **THEN** the Header SHALL not expose protected booking inbox data and SHALL keep existing login/register navigation

#### Scenario: Booking inbox navigation is active
- **WHEN** the user is on the booking inbox route
- **THEN** the Header SHALL mark the booking inbox navigation entry as active using the existing active-link behavior

### Requirement: Booking inbox route is protected
The frontend SHALL protect the booking inbox route using the existing authentication guard.

#### Scenario: Guest navigates directly to inbox
- **WHEN** a guest opens the booking inbox route directly
- **THEN** the frontend SHALL redirect through the existing login flow rather than calling protected booking APIs

#### Scenario: Authenticated user navigates directly to inbox
- **WHEN** an authenticated user opens the booking inbox route directly
- **THEN** the frontend SHALL render the booking inbox page within the main layout
