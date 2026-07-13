## MODIFIED Requirements

### Requirement: Public API operation coverage
The OpenAPI document SHALL include public RoomHub operations under `/api/**` and SHALL describe their request parameters, response status codes and response schemas without changing runtime business responses beyond explicitly versioned additive fields.

#### Scenario: Listings operation is documented
- **WHEN** a client reads the OpenAPI document
- **THEN** `paths./api/listings.get` is present
- **AND** its successful response is an array whose item schema represents `ListingResponseDto`

#### Scenario: Listings fields remain compatible
- **WHEN** a frontend developer inspects the successful `GET /api/listings` item schema
- **THEN** it contains `id`, `title`, `city`, `pricePerHour`, `capacity`, `spaceType`, `imageUrl`, `description`, `address`, `ownerOrganizationName`, `latitude`, and `longitude` using camelCase names
- **AND** adding coordinate support does not remove or rename existing fields in the actual listings JSON response

## ADDED Requirements

### Requirement: Listing coordinate fields are published in OpenAPI
Runtime OpenAPI SHALL document `latitude` and `longitude` on listing response schemas as nullable decimal coordinate fields, and the exported `openapi/roomhub-b2b.openapi.json` file SHALL contain the same coordinate contract.

#### Scenario: Public listing response schema documents coordinates
- **WHEN** a frontend developer inspects `ListingResponseDto` in `GET /api/openapi`
- **THEN** the schema includes nullable `latitude` and `longitude` fields
- **AND** the fields are documented as geographic coordinates suitable for map marker placement

#### Scenario: Publication and edit responses document coordinates
- **WHEN** a frontend developer inspects the successful `POST /api/listings` and `PUT /api/listings/{listingId}` responses in `GET /api/openapi`
- **THEN** each successful response references `ListingResponseDto`
- **AND** that schema includes `latitude` and `longitude`

#### Scenario: Owned listing response schema documents coordinates
- **WHEN** a frontend developer inspects the successful `GET /api/listings/owned` item schema in `GET /api/openapi`
- **THEN** `OwnedListingResponseDto` includes nullable `latitude` and `longitude` fields

#### Scenario: Exported contract contains listing coordinate fields
- **WHEN** the OpenAPI export command is run after implementing listing geocoding
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same `latitude` and `longitude` response fields as the runtime OpenAPI document
