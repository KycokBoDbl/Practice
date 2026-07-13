## ADDED Requirements

### Requirement: Listing edit refreshes geocoded coordinates
The system SHALL refresh stored `latitude` and `longitude` with the configured Yandex Maps Geocoder when an authenticated owner edits a listing and changes either `city` or `address`.

#### Scenario: Address-changing edit updates coordinates
- **WHEN** an authenticated `LANDLORD` sends `PUT /api/listings/{listingId}` for an owned listing with valid fields and a changed `city` or `address` that Yandex Geocoder resolves
- **THEN** the system updates the listing fields
- **AND** the `listings` row stores the newly resolved `latitude` and `longitude`
- **AND** the response body includes the updated coordinate fields

#### Scenario: Non-address edit keeps existing coordinates
- **WHEN** an authenticated `LANDLORD` sends `PUT /api/listings/{listingId}` for an owned listing and changes only non-address fields such as `title`, `description`, `pricePerHour`, `capacity`, `spaceType`, or `imageUrl`
- **THEN** the system updates those fields
- **AND** the existing stored `latitude` and `longitude` remain unchanged

#### Scenario: Failed geocoding leaves listing unchanged
- **WHEN** an authenticated `LANDLORD` sends `PUT /api/listings/{listingId}` with a changed `city` or `address` but geocoding returns no usable coordinates or fails externally
- **THEN** the system returns a `ProblemDetail` error
- **AND** the listing fields and coordinates remain unchanged

### Requirement: Owned listing responses expose coordinates
The system SHALL include `latitude` and `longitude` in each item returned by `GET /api/listings/owned` so landlords can preview the same map location that public users see.

#### Scenario: Owner sees listing coordinates
- **WHEN** an authenticated `LANDLORD` calls `GET /api/listings/owned`
- **THEN** each owned listing item includes `latitude` and `longitude` fields
- **AND** listings that have not yet been geocoded may return `null` for both coordinate fields

### Requirement: Listing coordinates are not landlord-editable fields
The system MUST derive listing coordinates from `city` and `address` and MUST NOT accept `latitude` or `longitude` as client-controlled fields in `UpdateListingRequestDto`.

#### Scenario: Client cannot provide edit coordinates
- **WHEN** a client sends `PUT /api/listings/{listingId}` with `latitude` or `longitude` fields in the JSON request body
- **THEN** the system ignores those fields or rejects them according to existing request-body handling
- **AND** any changed stored coordinates come from backend geocoding, not from the client payload
