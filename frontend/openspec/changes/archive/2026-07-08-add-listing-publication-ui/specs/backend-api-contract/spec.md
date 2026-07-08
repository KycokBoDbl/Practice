## ADDED Requirements

### Requirement: Frontend consumes listing publication endpoint
The frontend SHALL publish landlord listings through the existing backend `POST /api/listings` endpoint without changing the backend request or response contract.

#### Scenario: Landlord submits listing publication request
- **WHEN** an authenticated landlord submits a valid publication form
- **THEN** the frontend SHALL send `title`, `description`, `city`, `address`, `pricePerHour`, `capacity`, `spaceType`, and `imageUrl` to `POST /api/listings`
- **AND** empty optional `description` or `imageUrl` values SHALL be sent as `null` or omitted only if compatible with the backend contract

#### Scenario: Publication succeeds
- **WHEN** `POST /api/listings` returns `201` with `ListingResponseDto`
- **THEN** the frontend SHALL consume the returned listing id and listing fields for success navigation or success messaging

#### Scenario: Publication validation fails
- **WHEN** the backend returns `400` for publication
- **THEN** the frontend SHALL map validation details into field-level or form-level messages where possible

#### Scenario: Publication is unauthorized or forbidden
- **WHEN** the backend returns `401` or `403` for publication
- **THEN** the frontend SHALL show an authentication or role restriction state without losing unrelated form context unnecessarily

### Requirement: Frontend handles nullable listing media and description
The frontend SHALL safely consume listing responses where `description` and `imageUrl` are nullable.

#### Scenario: Published listing has no image
- **WHEN** a listing response has `imageUrl` equal to `null` or an empty display value
- **THEN** catalog, detail, and publication-related UI SHALL show a stable placeholder instead of a broken image or runtime error

#### Scenario: Published listing has no description
- **WHEN** a listing response has `description` equal to `null`
- **THEN** listing detail UI SHALL render a safe empty or fallback description state
