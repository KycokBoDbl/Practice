## ADDED Requirements

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
