## Purpose

Defines frontend requirements for strongly typed domain values while preserving backend compatibility.

## Requirements

### Requirement: Space type values are strongly typed
The frontend SHALL represent known space type values with a TypeScript union, typed constant map, or equivalent strongly typed domain model.

#### Scenario: Known space type is used
- **WHEN** frontend code references a known space type
- **THEN** TypeScript SHALL validate that the referenced value is part of the known space type set

#### Scenario: Space type labels are mapped
- **WHEN** frontend code looks up a label for a known space type
- **THEN** TypeScript SHALL validate that the label map contains the known keys

### Requirement: Unknown backend space types remain display-safe
The frontend SHALL preserve a safe fallback for backend space type values that are not yet known to the frontend.

#### Scenario: Unknown space type is received
- **WHEN** listing data contains an unknown space type value
- **THEN** the UI SHALL still display a fallback value rather than failing at runtime

### Requirement: Domain typing changes preserve API compatibility
The frontend SHALL strengthen frontend TypeScript types without requiring an immediate backend response shape change.

#### Scenario: API listing data is consumed
- **WHEN** listing data is received from the existing API
- **THEN** the frontend SHALL continue to consume the response while applying stronger typing at the frontend boundary

### Requirement: Listing publication request is typed
The frontend SHALL represent listing publication request data with explicit TypeScript types compatible with the backend listing publication contract.

#### Scenario: Publication request is built
- **WHEN** frontend code constructs a listing publication request
- **THEN** TypeScript SHALL require `title`, `city`, `address`, `pricePerHour`, `capacity`, and `spaceType`
- **AND** TypeScript SHALL allow `description` and `imageUrl` to be nullable optional values

### Requirement: Nullable listing fields are modeled safely
The frontend SHALL model nullable listing response fields according to the backend contract.

#### Scenario: Listing media and description are consumed
- **WHEN** frontend code consumes a listing response
- **THEN** TypeScript SHALL allow `description` and `imageUrl` to be `null`
- **AND** UI code SHALL handle those values without non-null assumptions
