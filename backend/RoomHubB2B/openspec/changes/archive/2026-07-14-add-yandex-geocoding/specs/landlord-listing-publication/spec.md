## ADDED Requirements

### Requirement: Listing publication stores geocoded coordinates
The system SHALL geocode the submitted `city` and `address` with the configured Yandex Maps Geocoder before creating a listing, store the resolved `latitude` and `longitude` on the listing record, and include both fields in the `ListingResponseDto` returned by `POST /api/listings`.

#### Scenario: Published listing receives coordinates
- **WHEN** an authenticated `LANDLORD` sends `POST /api/listings` with valid listing fields and an address that Yandex Geocoder resolves
- **THEN** the system creates the listing with status `PUBLISHED`
- **AND** the `listings` row stores both `latitude` and `longitude`
- **AND** the response body includes numeric `latitude` and `longitude` fields for the created listing

#### Scenario: Unresolvable address is rejected
- **WHEN** an authenticated `LANDLORD` sends `POST /api/listings` with valid listing fields but Yandex Geocoder returns no usable coordinate result for the submitted `city` and `address`
- **THEN** the system returns `400 Bad Request` with `ProblemDetail`
- **AND** no listing is created

#### Scenario: Geocoder integration failure prevents incomplete listing
- **WHEN** an authenticated `LANDLORD` sends `POST /api/listings` with valid listing fields but the geocoder request times out, returns a non-success response, or returns an invalid payload
- **THEN** the system returns a `ProblemDetail` error
- **AND** no listing is created without coordinates

### Requirement: Listing coordinates are server-owned
The system MUST derive listing coordinates from the stored `city` and `address` and MUST NOT accept `latitude` or `longitude` as client-controlled fields in `CreateListingRequestDto`.

#### Scenario: Client cannot provide publication coordinates
- **WHEN** a client sends `POST /api/listings` with `latitude` or `longitude` fields in the JSON request body
- **THEN** the system ignores those fields or rejects them according to existing request-body handling
- **AND** any stored coordinates come from backend geocoding, not from the client payload
