## ADDED Requirements

### Requirement: Landlord lists owned published and hidden listings
The system SHALL provide `GET /api/listings/owned` that returns all listings owned by the authenticated landlord organization whose status is `PUBLISHED` or `ARCHIVED`.

#### Scenario: Owner sees published and hidden listings
- **WHEN** an authenticated `LANDLORD` calls `GET /api/listings/owned`
- **THEN** the system returns `200 OK` with an array of owned listings
- **AND** the array includes listings owned by the token `organizationId` with status `PUBLISHED`
- **AND** the array includes listings owned by the token `organizationId` with status `ARCHIVED`
- **AND** each item includes `id`, `title`, `city`, `pricePerHour`, `capacity`, `spaceType`, `imageUrl`, `description`, `address`, `ownerOrganizationName`, and `status`

#### Scenario: Other organizations' listings are excluded
- **WHEN** an authenticated `LANDLORD` calls `GET /api/listings/owned`
- **THEN** the response does not include listings whose `owner_organization_id` differs from the token `organizationId`

#### Scenario: Tenant cannot list landlord-owned listings
- **WHEN** an authenticated `TENANT` calls `GET /api/listings/owned`
- **THEN** the system returns `403 Forbidden`

#### Scenario: Missing authentication is rejected
- **WHEN** a client calls `GET /api/listings/owned` without a valid bearer access token
- **THEN** the system returns `401 Unauthorized`
