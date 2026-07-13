## ADDED Requirements

### Requirement: Project banner is visible only to guests
The frontend SHALL show the project information banner on the main browsing page only when the current user is not authenticated.

#### Scenario: Guest opens the main page
- **WHEN** an unauthenticated user opens the main browsing page
- **THEN** the frontend SHALL show the existing project information banner

#### Scenario: Authenticated user opens the main page
- **WHEN** an authenticated user opens the main browsing page
- **THEN** the frontend SHALL hide the project information banner

### Requirement: Catalog search exposes active filters while filtering is in use
The frontend SHALL keep the catalog filter UI discoverable during an active search and SHALL surface which filters are currently applied.

#### Scenario: User applies catalog filters
- **WHEN** the user submits search text or filter values in the catalog
- **THEN** the frontend SHALL keep the filter controls available until the user clears or replaces the active search criteria
- **AND** the frontend SHALL show the active filters used for the current search

#### Scenario: User clears catalog filters
- **WHEN** the user clears the current catalog search criteria
- **THEN** the frontend SHALL return the catalog to the unfiltered state
- **AND** the frontend SHALL remove the active-filter indication

### Requirement: Listings page refreshes automatically without a manual refresh action
The frontend SHALL keep the listings page up to date through automatic refresh while the page is open.

#### Scenario: Listings page is visible
- **WHEN** the user remains on the listings page
- **THEN** the frontend SHALL periodically refresh the visible listing data
- **AND** the frontend SHALL not require a manual refresh button for this flow

#### Scenario: Listings page is left
- **WHEN** the user leaves the listings page
- **THEN** the frontend SHALL stop the refresh cycle for that page

### Requirement: Duplicate booking applications are recoverable on the frontend
The frontend SHALL treat duplicate booking application responses as a recoverable conflict state and SHALL preserve the current booking form context.

#### Scenario: Backend rejects a duplicate booking application
- **WHEN** the booking creation request is rejected with a conflict that represents a duplicate application
- **THEN** the frontend SHALL show a booking-specific conflict message
- **AND** the frontend SHALL keep the current booking form values visible

#### Scenario: User adjusts the booking after a duplicate conflict
- **WHEN** the user changes the date or time after a duplicate application conflict
- **THEN** the frontend SHALL allow the user to submit the adjusted request without requiring a page reload

### Requirement: Listing detail calendar shows availability without duplicating booking-page controls
The frontend SHALL present the listing calendar as an availability viewer on the listing detail page while preserving the ability to inspect booked and available times.

#### Scenario: Listing detail page opens
- **WHEN** the user opens a listing detail page
- **THEN** the frontend SHALL show a calendar that makes current availability visible
- **AND** the frontend SHALL not duplicate the richer booking-page slot selection workflow on that page

#### Scenario: Availability data changes
- **WHEN** refreshed availability data differs from the previous result
- **THEN** the frontend SHALL update the visible availability state without changing the route or user flow
