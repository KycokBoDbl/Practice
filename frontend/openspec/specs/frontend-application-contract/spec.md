## Purpose

Defines frontend application requirements after re-analysis of the current React/Vite codebase and backend API contract.

## Requirements

### Requirement: Frontend keeps public marketplace browsing available
The frontend SHALL allow guests and authenticated users to browse published spaces without requiring login.

#### Scenario: Catalog route loads
- **WHEN** the user opens the root catalog route
- **THEN** the frontend SHALL load `GET /api/listings` and render published listings with search/filter behavior

#### Scenario: Listing detail route loads
- **WHEN** the user opens a listing detail route
- **THEN** the frontend SHALL resolve the listing from the shared listing loading boundary and show details without requiring authentication

### Requirement: Frontend uses backend availability data for calendar state
The frontend SHALL render booking calendar availability from `GET /api/listings/{listingId}/availability`.

#### Scenario: Calendar month is visible
- **WHEN** a booking calendar displays a month for a listing
- **THEN** the frontend SHALL request hourly busy intervals for the visible month using `YYYY-MM-DDTHH:00` local-time strings

#### Scenario: Busy intervals are returned
- **WHEN** backend availability marks intervals as busy
- **THEN** the frontend SHALL classify days and hours as available, partial, or booked using those intervals

### Requirement: Frontend protects authenticated-only UI
The frontend SHALL centralize auth state and route protection for screens that require an account.

#### Scenario: Guest opens profile
- **WHEN** a guest navigates to `/profile`
- **THEN** the frontend SHALL redirect to login after auth state is resolved

#### Scenario: Authenticated user sends API requests
- **WHEN** an access token is present and not locally expired
- **THEN** the frontend SHALL attach `Authorization: Bearer <accessToken>` through the shared API client

### Requirement: Frontend booking submission follows backend workflow
The frontend SHALL submit booking requests through the backend booking API when the user confirms a selected available slot.

#### Scenario: Tenant confirms an available slot
- **WHEN** an authenticated tenant chooses a listing, date, start hour, and duration and confirms booking
- **THEN** the frontend SHALL send `listingId`, `startAt`, and `endAt` to `POST /api/bookings`

#### Scenario: Guest confirms a booking selection
- **WHEN** a guest attempts to confirm booking
- **THEN** the frontend SHALL route the user to login or registration before sending a protected booking request

#### Scenario: Backend returns booking conflict
- **WHEN** `POST /api/bookings` or a booking transition returns `409`
- **THEN** the frontend SHALL show a recoverable conflict state and refresh availability for the affected listing

### Requirement: Frontend exposes booking state to participants
The frontend SHALL be able to show authenticated booking status and history returned by the backend.

#### Scenario: Participant opens an existing booking
- **WHEN** the authenticated tenant or landlord opens a booking they participate in
- **THEN** the frontend SHALL load `GET /api/bookings/{bookingId}` and render status, price, selected interval, and confirmation deadline when present

#### Scenario: Participant reviews booking history
- **WHEN** booking history is shown
- **THEN** the frontend SHALL consume `GET /api/bookings/{bookingId}/history` as an append-only transition list
