## ADDED Requirements

### Requirement: Listing publication request is typed
The frontend SHALL represent listing publication request data with explicit TypeScript types compatible with the backend `CreateListingRequestDto`.

#### Scenario: Publication request is built
- **WHEN** frontend code builds a listing publication request
- **THEN** TypeScript SHALL validate `title`, `city`, `address`, `pricePerHour`, `capacity`, and `spaceType` as required fields
- **AND** TypeScript SHALL allow `description` and `imageUrl` to be nullable optional publication values

#### Scenario: Known space type is selected
- **WHEN** the publication form provides space type options
- **THEN** the options SHALL use the existing known `SpaceType` labels and values

### Requirement: Listing response nullable fields are modeled safely
The frontend SHALL model nullable listing response fields according to the current backend OpenAPI contract.

#### Scenario: Listing data is consumed
- **WHEN** frontend code consumes a listing response
- **THEN** TypeScript SHALL allow `description` and `imageUrl` to be `null`
- **AND** UI code SHALL handle those values without non-null assumptions
