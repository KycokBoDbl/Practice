## ADDED Requirements

### Requirement: Authenticated participant booking inbox page
The frontend SHALL provide an authenticated booking inbox page that lists bookings visible to the current participant organization.

#### Scenario: Authenticated user opens booking inbox
- **WHEN** an authenticated user opens the booking inbox route
- **THEN** the frontend SHALL call `GET /api/bookings` through the shared booking API client
- **THEN** the frontend SHALL render booking inbox items returned by the backend

#### Scenario: Guest opens booking inbox
- **WHEN** a guest opens the booking inbox route
- **THEN** the frontend SHALL route the user through the existing authentication guard before loading protected booking data

#### Scenario: Inbox item links to detail
- **WHEN** a booking inbox item is rendered
- **THEN** it SHALL link to the existing booking detail route for that booking id

### Requirement: Booking inbox item content
The booking inbox page SHALL render each booking with enough context to understand status, counterparty, listing, interval, and cost.

#### Scenario: Booking item is shown
- **WHEN** an inbox item is displayed
- **THEN** the frontend SHALL show listing title, booking status, start/end interval, total price, relevant organization names, and last update time when available

#### Scenario: Role-specific counterparty is shown
- **WHEN** the current profile role is `TENANT`
- **THEN** the frontend SHALL make the landlord organization visible for each item when provided

#### Scenario: Landlord reads incoming request
- **WHEN** the current profile role is `LANDLORD`
- **THEN** the frontend SHALL make the tenant organization visible for each item when provided

### Requirement: Booking inbox states
The booking inbox page SHALL provide stable UI states for loading, empty results, errors, and successful results.

#### Scenario: Inbox is loading
- **WHEN** the booking inbox request is pending
- **THEN** the frontend SHALL show a loading state without clearing authenticated layout navigation

#### Scenario: Inbox is empty
- **WHEN** the backend returns an empty list
- **THEN** the frontend SHALL show a role-appropriate empty state without presenting it as an error

#### Scenario: Inbox load fails
- **WHEN** the backend returns a recoverable error for the inbox request
- **THEN** the frontend SHALL show an error state with a retry path

### Requirement: Status change awareness without notifications
The booking inbox SHALL make booking status changes discoverable without using notification badges or unread counters.

#### Scenario: Recently changed bookings are visible
- **WHEN** the backend returns inbox items with `updatedAt`
- **THEN** the frontend SHALL show the update time and preserve backend recency order unless the user applies a filter

#### Scenario: User refreshes inbox
- **WHEN** the user triggers refresh on the inbox page
- **THEN** the frontend SHALL reload `GET /api/bookings` and update visible statuses from the backend response

#### Scenario: User returns from detail after action
- **WHEN** the user navigates back to the inbox after a booking transition in detail
- **THEN** the inbox SHALL load fresh backend data rather than relying on stale previously rendered items

### Requirement: Inbox filters support action discovery
The booking inbox page SHALL support simple status grouping that helps users find bookings needing attention.

#### Scenario: Action-required filter is selected
- **WHEN** a landlord selects an action-required view
- **THEN** the frontend SHALL prioritize or filter bookings with status `REQUESTED`

#### Scenario: Tenant action-required filter is selected
- **WHEN** a tenant selects an action-required view
- **THEN** the frontend SHALL prioritize or filter bookings with status `AWAITING_CONFIRMATION`

#### Scenario: All bookings view is selected
- **WHEN** the user selects all bookings
- **THEN** the frontend SHALL show all inbox items returned by the backend for the current role
