## ADDED Requirements

### Requirement: AI listing search contract is published
Runtime OpenAPI SHALL describe `POST /api/listings/ai-search` as a public operation that accepts an AI search prompt request, returns an array of `ListingResponseDto`, and documents validation and upstream integration errors. The exported `openapi/roomhub-b2b.openapi.json` file SHALL contain the same contract.

#### Scenario: AI search operation is present in runtime OpenAPI
- **WHEN** a client reads `GET /api/openapi`
- **THEN** `paths./api/listings/ai-search.post` is present
- **AND** the operation does not require bearer security
- **AND** the request body references an AI listing search request schema with required `prompt`
- **AND** response `200` is documented as an array whose item schema represents `ListingResponseDto`
- **AND** responses `400` and `502` are documented as `application/problem+json` errors

#### Scenario: AI search request schema is documented
- **WHEN** a frontend developer inspects the AI listing search request schema in `GET /api/openapi`
- **THEN** the schema contains `prompt` using camelCase naming
- **AND** the schema documents prompt length and non-blank validation constraints
- **AND** the schema does not expose GigaChat credentials, token fields, or extracted filter internals as client-controlled fields

#### Scenario: Exported contract contains AI search changes
- **WHEN** the OpenAPI export command is run after implementing AI listing search
- **THEN** `openapi/roomhub-b2b.openapi.json` contains the same `POST /api/listings/ai-search` operation, request schema, response schema, and status codes as the runtime OpenAPI document
