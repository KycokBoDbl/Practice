## ADDED Requirements

### Requirement: Single-listing loading is centralized
The frontend SHALL centralize single-listing loading for listing detail and booking routes behind a shared API function, hook, or helper.

#### Scenario: Detail page loads a listing
- **WHEN** `SpacePage` needs listing data for a route `id`
- **THEN** it SHALL use the shared single-listing loading boundary rather than duplicating list-and-find logic inside the page

#### Scenario: Booking page loads a listing
- **WHEN** `BookingPage` needs listing data for a route `id`
- **THEN** it SHALL use the shared single-listing loading boundary rather than duplicating list-and-find logic inside the page

### Requirement: Single-listing loading supports API endpoint or fallback
The frontend SHALL support a future direct single-listing API call while preserving current behavior if only list loading is available.

#### Scenario: Direct endpoint is available
- **WHEN** a direct listing-by-id endpoint is available
- **THEN** the shared loading boundary SHALL be able to load a listing by id through that endpoint

#### Scenario: Direct endpoint is unavailable
- **WHEN** no direct listing-by-id endpoint is available
- **THEN** the shared loading boundary SHALL centralize the existing list-and-find fallback

### Requirement: BookingPage distinguishes loading states
`BookingPage` SHALL distinguish loading, error, not found, and loaded states when resolving its listing.

#### Scenario: Listing request is pending
- **WHEN** `BookingPage` is waiting for listing data
- **THEN** it SHALL render a loading state rather than the not-found state

#### Scenario: Listing is missing after load
- **WHEN** listing loading completes and no listing matches the route id
- **THEN** `BookingPage` SHALL render the not-found state

#### Scenario: Listing load fails
- **WHEN** listing loading fails because of an API or network error
- **THEN** `BookingPage` SHALL render an error state distinct from not-found

#### Scenario: Listing loads successfully
- **WHEN** listing loading completes successfully
- **THEN** `BookingPage` SHALL render the booking page content for that listing
