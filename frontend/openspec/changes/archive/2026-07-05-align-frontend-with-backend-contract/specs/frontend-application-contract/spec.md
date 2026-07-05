## ADDED Requirements

### Requirement: Frontend booking flow is backend-backed
The frontend SHALL complete the marketplace booking flow by submitting selected booking data to the existing backend booking workflow.

#### Scenario: Booking confirmation is submitted
- **WHEN** the user confirms a selected listing, date, start hour, and duration from the booking route
- **THEN** the frontend SHALL submit a backend booking request or route the user to authentication if no valid user is present

#### Scenario: Booking result is returned
- **WHEN** the backend returns a booking response after creation or transition
- **THEN** the frontend SHALL update the visible booking state from the backend response rather than deriving workflow status locally

#### Scenario: Booking availability changes
- **WHEN** booking creation or transition can affect listing availability
- **THEN** the frontend SHALL refresh availability for the visible calendar period

### Requirement: Frontend handles backend booking errors consistently
The frontend SHALL map backend booking errors into user-visible states using shared API error parsing where possible.

#### Scenario: Booking request is unauthorized
- **WHEN** the backend returns `401` for a booking endpoint
- **THEN** the frontend SHALL treat the user as unauthenticated and present a login path

#### Scenario: Booking request is forbidden
- **WHEN** the backend returns `403` for a booking endpoint
- **THEN** the frontend SHALL show that the current account cannot perform the requested booking action

#### Scenario: Booking or listing is unavailable
- **WHEN** the backend returns `404` for a booking endpoint
- **THEN** the frontend SHALL show a not-found or unavailable state without exposing protected details

#### Scenario: Booking request conflicts
- **WHEN** the backend returns `409` for a booking endpoint
- **THEN** the frontend SHALL show that the booking state or calendar slot changed and prompt the user to choose an updated action
