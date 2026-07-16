## ADDED Requirements

### Requirement: Backend exposes public AI listing search for frontend consumption
The backend SHALL provide a public AI-assisted listing search endpoint that the frontend can call without changing existing listing DTO assumptions.

#### Scenario: AI listing search is requested
- **WHEN** the frontend posts `{ "prompt": "<text>" }` to `/api/listings/ai-search`
- **THEN** the backend SHALL return a JSON array of standard published listing response objects
- **AND** the frontend SHALL treat the response as `Listing[]`

#### Scenario: AI listing search prompt is invalid
- **WHEN** the AI listing search prompt is blank, malformed, or violates backend validation
- **THEN** the backend SHALL return a ProblemDetail-compatible `400` response
- **AND** the frontend SHALL parse it as a recoverable AI-search input error

#### Scenario: AI provider fails
- **WHEN** GigaChat OAuth, model request, timeout, or model response parsing fails
- **THEN** the backend SHALL return a ProblemDetail-compatible `502` response
- **AND** the frontend SHALL parse it as temporary AI-search unavailability

#### Scenario: AI listing search does not expose parsed filter metadata
- **WHEN** the backend returns AI search results
- **THEN** the frontend SHALL NOT depend on parsed filter, explanation, confidence, or ranking metadata that is not present in the API response
