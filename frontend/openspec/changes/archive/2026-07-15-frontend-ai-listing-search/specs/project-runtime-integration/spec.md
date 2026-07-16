## ADDED Requirements

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
