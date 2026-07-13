# frontend-listing-map-view Specification

## Purpose
TBD - created by archiving change add-listing-map-view. Update Purpose after archive.
## Requirements
### Requirement: Listing detail exposes a map action when coordinates are available
The frontend SHALL provide a user-visible map action on the listing detail page only when the listing has both latitude and longitude from the existing listing data.

#### Scenario: Listing has coordinates
- **WHEN** a listing detail page is rendered with non-null `latitude` and `longitude`
- **THEN** the frontend SHALL expose a `Посмотреть на карте` action for that listing detail page

#### Scenario: Listing has missing coordinates
- **WHEN** a listing detail page is rendered without either `latitude` or `longitude`
- **THEN** the frontend SHALL not open an empty or incorrectly centered map for that listing
- **AND** the frontend SHALL show or expose a clear unavailable state for map viewing

### Requirement: Map action opens a focused listing map view
The frontend SHALL open a Yandex Maps view focused on the selected listing coordinates when the user activates the map action.

#### Scenario: User opens a listing map
- **WHEN** the user activates `Посмотреть на карте` for a listing with coordinates on the listing detail page
- **THEN** the frontend SHALL open a map view centered on that listing's latitude and longitude
- **AND** the frontend SHALL show a marker for the selected listing

#### Scenario: Listing context is shown on the map
- **WHEN** the map view is open for a listing
- **THEN** the frontend SHALL show the listing title and address near the map or marker

#### Scenario: User closes the map
- **WHEN** the user closes the listing map view
- **THEN** the frontend SHALL return to the previous listing detail context without changing the existing route workflow

### Requirement: Map view uses backend-provided coordinates without frontend geocoding
The frontend SHALL use `latitude` and `longitude` from existing listing responses as the source of map positioning and SHALL NOT geocode address text in the browser.

#### Scenario: Listing address is available as text
- **WHEN** the frontend renders a map for a listing
- **THEN** it SHALL use the listing coordinates returned by the backend
- **AND** it SHALL NOT call a frontend geocoding endpoint or provider using the address text

#### Scenario: Coordinates are absent
- **WHEN** a listing has an address but no coordinates
- **THEN** the frontend SHALL treat map viewing as unavailable for that listing
- **AND** it SHALL NOT attempt to infer coordinates from the address on the client

### Requirement: Map integration preserves existing listing workflows
The frontend SHALL add map viewing without changing existing listing, booking, publication, or management workflows.

#### Scenario: Listing detail is opened
- **WHEN** a user opens a listing detail page
- **THEN** existing detail, availability, and booking entry behavior SHALL remain available
- **AND** this change SHALL add only the map action on the detail page

#### Scenario: Backend API is used
- **WHEN** the map integration needs listing location data
- **THEN** it SHALL use the existing listing response shape from the frontend listing API functions
- **AND** it SHALL NOT require a new backend endpoint or backend API contract change

### Requirement: Map loading failures are recoverable
The frontend SHALL handle map provider loading failures without breaking listing browsing.

#### Scenario: Map provider fails to load
- **WHEN** the user opens a map view and the map provider or tiles cannot load
- **THEN** the frontend SHALL show a recoverable map error state
- **AND** the frontend SHALL keep the listing title and address visible

#### Scenario: User exits failed map view
- **WHEN** the map view is in an error state
- **THEN** the user SHALL be able to close it and continue using the listing page

