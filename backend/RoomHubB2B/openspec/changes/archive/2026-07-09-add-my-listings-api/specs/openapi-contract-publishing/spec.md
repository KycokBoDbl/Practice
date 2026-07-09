## ADDED Requirements

### Requirement: Owned listings contract is published
Runtime OpenAPI SHALL describe `GET /api/listings/owned` as a protected landlord listing management operation returning the authenticated landlord organization's published and hidden listings.

#### Scenario: Owned listings operation is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** `paths./api/listings/owned.get` is present and requires bearer security
- **AND** response `200` is documented as an array of owned listing management items
- **AND** responses `401` and `403` are documented as errors using `application/problem+json`

#### Scenario: Owned listing response schema is documented
- **WHEN** a frontend developer inspects the successful `GET /api/listings/owned` item schema
- **THEN** the schema documents `id`, `title`, `city`, `pricePerHour`, `capacity`, `spaceType`, `imageUrl`, `description`, `address`, `ownerOrganizationName`, and `status` using camelCase names
- **AND** `status` is documented as the listing lifecycle status with values including `PUBLISHED` and `ARCHIVED`
- **AND** the schema does not expose `ownerOrganizationId`

#### Scenario: Exported contract contains owned listings changes
- **WHEN** the OpenAPI export command is run after implementing owned listing retrieval
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same `GET /api/listings/owned` operation and response schema as the runtime OpenAPI document
