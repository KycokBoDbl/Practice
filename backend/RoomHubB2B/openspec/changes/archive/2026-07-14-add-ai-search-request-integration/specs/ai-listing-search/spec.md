## ADDED Requirements

### Requirement: Natural-language listing search endpoint
The system SHALL provide public `POST /api/listings/ai-search` accepting a JSON body with a `prompt` field and returning an array of `ListingResponseDto` items for matching published listings. The endpoint MUST NOT require bearer authentication and MUST NOT change `GET /api/listings`.

#### Scenario: Tenant searches with a prompt
- **WHEN** a client sends `POST /api/listings/ai-search` with `{"prompt":"Нужен конференц-зал в Москве на 30 человек до 5000 рублей в час"}`
- **THEN** the system responds `200 OK`
- **AND** the response body is a JSON array of listing items using the existing `ListingResponseDto` field names
- **AND** every returned listing has status `PUBLISHED` internally

#### Scenario: Existing listing catalog remains unchanged
- **WHEN** a client calls `GET /api/listings` after the AI search endpoint is added
- **THEN** the endpoint still returns the existing public listing array without requiring a prompt or authentication

### Requirement: Prompt request validation
The system SHALL reject invalid AI search requests before calling GigaChat. The `prompt` field MUST be present, MUST contain non-blank text after trimming, and MUST be no longer than the configured prompt length limit.

#### Scenario: Blank prompt is rejected
- **WHEN** a client sends `POST /api/listings/ai-search` with a missing, null, empty, or blank `prompt`
- **THEN** the system responds `400 Bad Request`
- **AND** no GigaChat request is sent
- **AND** no listing query is executed

#### Scenario: Prompt is too long
- **WHEN** a client sends a `prompt` longer than the configured maximum length
- **THEN** the system responds `400 Bad Request`
- **AND** the response uses `application/problem+json`

### Requirement: GigaChat filter extraction
The system SHALL use GigaChat API to transform a valid prompt into a structured listing filter. The extracted filter MUST be limited to `city`, `spaceType`, `minCapacity`, `maxPricePerHour`, and `limit`.

#### Scenario: Supported filter fields are extracted
- **WHEN** GigaChat returns valid JSON containing `city`, `spaceType`, `minCapacity`, `maxPricePerHour`, and `limit`
- **THEN** the system validates those fields against backend rules
- **AND** the validated filter is used for the listing search

#### Scenario: Prompt has only partial filter information
- **WHEN** GigaChat returns valid JSON where one or more supported fields are null or absent
- **THEN** the system treats absent criteria as unrestricted filters
- **AND** the listing query still executes with the criteria that were extracted

#### Scenario: Model returns invalid filter output
- **WHEN** GigaChat returns malformed JSON, prose instead of JSON, an unknown `spaceType`, negative numeric values, or a `limit` outside allowed bounds
- **THEN** the system responds `502 Bad Gateway`
- **AND** no unvalidated model value is used in the listing query

### Requirement: GigaChat authentication and upstream failures
The system SHALL obtain a GigaChat access token with the configured authorization key before making the model request and SHALL treat OAuth or model request failures as upstream integration failures.

#### Scenario: Access token is requested before model call
- **WHEN** a valid AI search request is processed and no reusable GigaChat access token is available
- **THEN** the system requests an access token from the configured OAuth endpoint using the configured scope
- **AND** the model request uses the returned access token as bearer authentication

#### Scenario: Upstream request fails
- **WHEN** the GigaChat OAuth endpoint or model endpoint times out, returns a non-success response, or returns an empty unusable body
- **THEN** the system responds `502 Bad Gateway`
- **AND** the response uses `application/problem+json`
- **AND** GigaChat credentials and access tokens are not included in the response or application logs

### Requirement: Published listing filter application
The system SHALL apply the validated AI filter only to published listings. `city` MUST match case-insensitively, `spaceType` MUST match exactly, `capacity` MUST be greater than or equal to `minCapacity`, and `pricePerHour` MUST be less than or equal to `maxPricePerHour`.

#### Scenario: Matching listings are returned
- **WHEN** the validated filter is `city=Москва`, `spaceType=CONFERENCE_HALL`, `minCapacity=30`, and `maxPricePerHour=5000.00`
- **THEN** every returned listing is published
- **AND** every returned listing has city `Москва` ignoring case
- **AND** every returned listing has `spaceType=CONFERENCE_HALL`
- **AND** every returned listing has `capacity >= 30`
- **AND** every returned listing has `pricePerHour <= 5000.00`

#### Scenario: Archived listings are hidden from AI search
- **WHEN** a listing matches the extracted filter but has status `ARCHIVED`
- **THEN** the listing is not included in the AI search response

#### Scenario: No listings match
- **WHEN** the validated filter has no matching published listings
- **THEN** the system responds `200 OK`
- **AND** the response body is an empty JSON array

#### Scenario: Result limit is enforced
- **WHEN** the validated filter contains a `limit`
- **THEN** the system returns no more than that number of listings
- **AND** the applied limit does not exceed the backend configured maximum
