## ADDED Requirements

### Requirement: Listing management contract is published
Runtime OpenAPI SHALL describe the protected landlord listing management operations, including full edit, hide, reactivate, and delete endpoints, request and response schemas, bearer security, and documented error responses. The exported `openapi/roomhub-b2b.openapi.json` file SHALL contain the same contract.

#### Scenario: Edit operation is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** `paths./api/listings/{listingId}.put` is present and requires bearer security
- **AND** the path documents a required `listingId` path parameter
- **AND** the request body references the listing update request schema
- **AND** response `200` references `ListingResponseDto`
- **AND** responses `400`, `401`, `403`, and `404` are documented as errors

#### Scenario: Hide operation is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** `paths./api/listings/{listingId}/hide.post` is present and requires bearer security
- **AND** the path documents a required `listingId` path parameter
- **AND** response `204` is documented with no response body
- **AND** responses `401`, `403`, and `404` are documented as errors

#### Scenario: Reactivate operation is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** `paths./api/listings/{listingId}/activate.post` is present and requires bearer security
- **AND** the path documents a required `listingId` path parameter
- **AND** response `204` is documented with no response body
- **AND** responses `401`, `403`, and `404` are documented as errors

#### Scenario: Delete operation is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** `paths./api/listings/{listingId}.delete` is present and requires bearer security
- **AND** the path documents a required `listingId` path parameter
- **AND** response `204` is documented with no response body
- **AND** responses `401`, `403`, `404`, and `409` are documented as errors

#### Scenario: Listing management schemas use frontend-compatible field names
- **WHEN** a frontend developer inspects the listing management schemas
- **THEN** listing request and response fields use existing camelCase names
- **AND** `ListingResponseDto` continues to expose `ownerOrganizationId`
- **AND** management request schemas do not expose `ownerOrganizationId`, `status`, or `createdAt` as writable fields

#### Scenario: Exported contract contains listing management changes
- **WHEN** the OpenAPI export command is run after implementing landlord listing management
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same edit, hide, reactivate, and delete operations and schemas as the runtime OpenAPI document
