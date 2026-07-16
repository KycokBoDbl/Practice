## Purpose

Defines whole-project requirements for running RoomHub B2B as an integrated frontend, backend, and PostgreSQL application.

## Requirements

### Requirement: Project starts as a composed system
The project SHALL support local startup through the root Docker Compose configuration with PostgreSQL, backend, and frontend services.

#### Scenario: Required auth secret is provided
- **WHEN** `ROOMHUB_AUTH_TOKEN_SECRET` is set and `docker compose up --build` is executed from the project root
- **THEN** PostgreSQL SHALL be available to the backend, the backend SHALL listen on port `8081`, and the frontend SHALL be served on port `3000`

#### Scenario: Required auth secret is missing
- **WHEN** the project is started without `ROOMHUB_AUTH_TOKEN_SECRET`
- **THEN** startup SHALL fail before running the backend with an implicit or weak JWT signing secret

### Requirement: Frontend uses the configured backend base URL
The frontend SHALL route API calls through `VITE_API_URL` when configured and fall back to a local backend URL for development.

#### Scenario: Docker image is built
- **WHEN** the frontend Docker image is built by the root Compose file
- **THEN** `VITE_API_URL` SHALL point browser API calls at `http://localhost:8081`

#### Scenario: Local dev server is used
- **WHEN** `VITE_API_URL` is not configured for local frontend development
- **THEN** the frontend SHALL use `http://127.0.0.1:8081` as the backend base URL

### Requirement: Backend is the source of API truth
The project SHALL treat backend Spring MVC mappings, DTOs, validation, and OpenAPI annotations as the source of public API truth.

#### Scenario: API contract changes
- **WHEN** an endpoint, DTO, validation rule, status code, or security requirement changes
- **THEN** the backend OpenAPI document SHALL be regenerated and reviewed with the code change

#### Scenario: Frontend consumes backend data
- **WHEN** frontend API types or client calls are updated
- **THEN** they SHALL match the generated backend OpenAPI contract rather than inventing incompatible shapes

### Requirement: Public and authenticated surfaces are separated
The project SHALL keep public discovery endpoints available to guests and require bearer authentication for protected business operations.

#### Scenario: Guest browses public data
- **WHEN** a guest loads listings, listing availability, auth registration, auth login, or OpenAPI metadata
- **THEN** the backend SHALL allow the request without a bearer token

#### Scenario: User performs protected operations
- **WHEN** a request targets profile or booking workflow endpoints
- **THEN** the backend SHALL require a valid bearer token and the frontend SHALL send it when available

### Requirement: Error responses are contract-friendly
The project SHALL expose API errors in a shape the frontend can parse into form-level and field-level messages.

#### Scenario: Backend rejects invalid input
- **WHEN** validation, authentication, authorization, not-found, or conflict errors occur
- **THEN** the backend SHALL return ProblemDetail-compatible responses that preserve status and user-actionable detail

### Requirement: Integrated runtime provides GigaChat backend credentials
The project runtime SHALL provide the backend with a GigaChat authorization key through backend-only environment configuration.

#### Scenario: GigaChat key is configured
- **WHEN** the project is started for AI-search testing
- **THEN** `ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY` SHALL be available to the backend process
- **AND** the key SHALL NOT be exposed through frontend `VITE_*` variables or browser-bundled code

#### Scenario: GigaChat key is missing
- **WHEN** AI-search runtime configuration is missing `ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY`
- **THEN** the developer SHALL treat AI search as not ready for integrated manual verification
- **AND** frontend implementation SHALL still handle backend AI-search failure as a recoverable unavailable state

#### Scenario: Runtime file outside frontend requires editing
- **WHEN** enabling the GigaChat key requires editing root Docker Compose or backend-owned files
- **THEN** implementation SHALL stop and request explicit user approval before making that non-frontend change
