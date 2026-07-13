## ADDED Requirements

### Requirement: Participants can discover their bookings
The frontend booking workflow SHALL allow authenticated participants to discover booking requests returned by the backend inbox without knowing booking ids in advance.

#### Scenario: Tenant discovers outgoing requests
- **WHEN** an authenticated tenant opens the booking inbox
- **THEN** the frontend SHALL show bookings returned for that tenant organization and allow opening their detail pages

#### Scenario: Landlord discovers incoming requests
- **WHEN** an authenticated landlord opens the booking inbox
- **THEN** the frontend SHALL show bookings returned for listings owned by that landlord organization and allow opening their detail pages

#### Scenario: Booking creation result leads to inbox-compatible detail
- **WHEN** a tenant creates a booking and receives a booking id
- **THEN** the existing detail route SHALL remain compatible with the booking inbox item links

### Requirement: Booking status changes are visible through workflow refresh
The frontend booking workflow SHALL expose status changes through fresh backend reads after users revisit the inbox or complete booking actions.

#### Scenario: Transition updates detail and inbox path
- **WHEN** a participant performs a booking transition in detail
- **THEN** the detail page SHALL continue to refresh booking state and history from the backend
- **THEN** returning to the inbox SHALL show the backend-updated status

#### Scenario: Backend status changed outside current page
- **WHEN** a booking status changes while the user is not on the detail page
- **THEN** opening or refreshing the inbox SHALL show the latest status returned by `GET /api/bookings`
