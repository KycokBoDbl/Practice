# OpenAPI Contract Publishing Specification

## Purpose

Defines the runtime OpenAPI contract, public operation coverage, reproducible contract export, and the documented workflow for coordinating backend and frontend API changes.
## Requirements
### Requirement: Runtime OpenAPI document
Backend SHALL expose a valid OpenAPI 3 JSON document through `GET /api/openapi` while the application is running.

#### Scenario: Frontend developer reads the runtime contract
- **WHEN** a client sends `GET /api/openapi` with `Accept: application/json`
- **THEN** backend returns HTTP 200 with a JSON document whose `openapi` value identifies OpenAPI version 3
- **AND** the document metadata identifies `RoomHub B2B API`

### Requirement: Public API operation coverage
The OpenAPI document SHALL include public RoomHub operations under `/api/**` and SHALL describe their request parameters, response status codes and response schemas without changing runtime business responses.

#### Scenario: Listings operation is documented
- **WHEN** a client reads the OpenAPI document
- **THEN** `paths./api/listings.get` is present
- **AND** its successful response is an array whose item schema represents `ListingResponseDto`

#### Scenario: Listings fields remain compatible
- **WHEN** a frontend developer inspects the successful `GET /api/listings` item schema
- **THEN** it contains `id`, `title`, `city`, `pricePerHour`, `capacity`, `spaceType`, `imageUrl`, `description` and `address` using camelCase names
- **AND** adding OpenAPI support does not remove, rename or add fields in the actual listings JSON response

### Requirement: Reproducible file export
The project SHALL provide a documented Maven Wrapper command that obtains the OpenAPI document from a fully started backend and writes it to `openapi/roomhub-b2b.openapi.json`.

#### Scenario: Successful export
- **WHEN** PostgreSQL is available with the documented datasource configuration and a developer runs the OpenAPI export command
- **THEN** Maven starts the application on the configured export port, saves a valid OpenAPI JSON file at `openapi/roomhub-b2b.openapi.json` and stops the application
- **AND** the exported contract contains the same paths and schemas as `GET /api/openapi`

#### Scenario: Export cannot obtain the contract
- **WHEN** the application cannot start or the OpenAPI endpoint cannot be read
- **THEN** the export command exits with a non-zero status instead of leaving a stale export reported as successful

#### Scenario: Contract is unchanged
- **WHEN** the export command is run twice without changes to controllers, DTOs or OpenAPI metadata
- **THEN** the second generated file has no semantic contract diff from the first file

### Requirement: Documented contract update pipeline
The root `README.md` SHALL document prerequisites, backend startup, the runtime OpenAPI URL, the export commands for Windows and Unix, the generated file location and the regular workflow shared by backend and frontend developers.

#### Scenario: Backend API contract changes
- **WHEN** a backend developer adds or changes a public endpoint, parameter, status code or DTO field
- **THEN** the documented workflow requires updating the relevant OpenAPI metadata, running tests, exporting the contract and reviewing its diff in the same change
- **AND** the frontend developer can consume the committed JSON file for type or client generation

#### Scenario: Implementation does not change the contract
- **WHEN** backend internals change without changing public API behavior
- **THEN** the documented workflow uses an unchanged generated file as evidence that no frontend contract update is required

### Requirement: Контракт публикации объявления
Runtime OpenAPI SHALL описывать `POST /api/listings` как защищенную bearer authentication операцию публикации объявления, включая `CreateListingRequestDto`, успешный `ListingResponseDto`, nullable `description`/`imageUrl` и ответы `201`, `400`, `401` и `403`; ошибки SHALL использовать `application/problem+json`, а экспортируемый файл SHALL содержать тот же контракт.

#### Scenario: Операция публикации присутствует в runtime OpenAPI
- **WHEN** клиент читает `GET /api/openapi`
- **THEN** `paths./api/listings.post` присутствует и требует bearer security
- **AND** request body ссылается на `CreateListingRequestDto`
- **AND** ответ `201` ссылается на `ListingResponseDto`, а ответы `400`, `401` и `403` документированы как ошибки
- **AND** `description` и `imageUrl` в `ListingResponseDto` допускают `null`, а error responses описаны с media type `application/problem+json`

#### Scenario: Экспортированный контракт обновлен
- **WHEN** разработчик выполняет Maven profile `openapi-export` после добавления публикации
- **THEN** `openapi/roomhub-b2b.openapi.json` содержит тот же `POST /api/listings`, request/response schemas, security requirement и status codes, что runtime OpenAPI

### Requirement: Booking inbox contract is published
The OpenAPI document SHALL include the booking inbox endpoint, its supported query parameters, successful response schema, and documented error responses.

#### Scenario: Inbox endpoint is present in OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** the OpenAPI document contains the booking inbox path
- **THEN** the operation documents bearer authentication, `200`, `400`, and `401` responses

#### Scenario: Inbox response schema is documented
- **WHEN** a frontend developer inspects the successful inbox response schema
- **THEN** each item schema documents id, listingId, listingTitle, status, startAt, endAt, pricePerHour, totalPrice, confirmationDeadline, tenantOrganizationName, landlordOrganizationName, createdAt, and updatedAt using camelCase names

#### Scenario: Exported contract contains inbox changes
- **WHEN** the OpenAPI export command is run after implementing the booking inbox endpoint
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same booking inbox path and schemas as the runtime OpenAPI document

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
- **AND** `ListingResponseDto` exposes `ownerOrganizationName` and does not expose `ownerOrganizationId`
- **AND** management request schemas do not expose `ownerOrganizationId`, `status`, or `createdAt` as writable fields

#### Scenario: Exported contract contains listing management changes
- **WHEN** the OpenAPI export command is run after implementing landlord listing management
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same edit, hide, reactivate, and delete operations and schemas as the runtime OpenAPI document

