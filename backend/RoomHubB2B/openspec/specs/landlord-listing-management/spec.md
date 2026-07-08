# Landlord Listing Management Specification

## Purpose

Defines authenticated owner-only editing, visibility lifecycle management, and safe deletion of landlord listings.

## Requirements

### Requirement: Landlord edits an owned listing
The system SHALL provide `PUT /api/listings/{listingId}` that allows an authenticated `LANDLORD` to replace mutable fields of a listing owned by the landlord organization. The request MUST validate the same business fields as listing publication and MUST NOT accept `ownerOrganizationId`, `status`, or `createdAt` as client-controlled fields.

#### Scenario: Successful edit
- **WHEN** an authenticated `LANDLORD` sends `PUT /api/listings/{listingId}` for a listing whose `owner_organization_id` matches the token `organizationId` with valid `title`, `description`, `city`, `address`, `pricePerHour`, `capacity`, `spaceType`, and `imageUrl`
- **THEN** the system updates the listing fields and returns `200 OK` with `ListingResponseDto`
- **AND** the listing keeps its original owner organization, status, and creation timestamp

#### Scenario: Optional fields are cleared
- **WHEN** an authenticated owner sends `PUT /api/listings/{listingId}` with valid required fields and `description` or `imageUrl` set to `null`
- **THEN** the system stores `null` for those optional fields and returns the updated listing

#### Scenario: Edit request is invalid
- **WHEN** an authenticated owner sends `PUT /api/listings/{listingId}` with blank required text, non-positive price, non-positive capacity, unsupported `spaceType`, or a non-HTTP(S) `imageUrl`
- **THEN** the system returns `400 Bad Request` with `ProblemDetail`
- **AND** the listing is not changed

### Requirement: Listing management is restricted to the owning landlord
The system MUST allow listing edit, hide, reactivate, and delete operations only for authenticated users with role `LANDLORD` whose token `organizationId` matches the listing owner organization.

#### Scenario: Tenant cannot manage listings
- **WHEN** an authenticated `TENANT` calls a listing management endpoint
- **THEN** the system returns `403 Forbidden`
- **AND** no listing is changed or deleted

#### Scenario: Landlord cannot manage another organization's listing
- **WHEN** an authenticated `LANDLORD` calls a listing management endpoint for a listing owned by another organization
- **THEN** the system returns `404 Not Found`
- **AND** no listing is changed or deleted

#### Scenario: Missing authentication is rejected
- **WHEN** a client calls a listing management endpoint without a valid bearer access token
- **THEN** the system returns `401 Unauthorized`
- **AND** no listing is changed or deleted

### Requirement: Landlord hides an owned listing
The system SHALL provide `POST /api/listings/{listingId}/hide` that changes an owned listing from public catalog visibility to a hidden state represented by listing status `ARCHIVED`.

#### Scenario: Successful hide
- **WHEN** an authenticated owner sends `POST /api/listings/{listingId}/hide` for a `PUBLISHED` listing
- **THEN** the system sets the listing status to `ARCHIVED`
- **AND** the system returns `204 No Content`

#### Scenario: Hidden listing is removed from public discovery
- **WHEN** a listing has status `ARCHIVED`
- **THEN** public `GET /api/listings` does not include that listing
- **AND** public listing availability lookup and new booking creation treat the listing as unavailable or not found

#### Scenario: Hiding is idempotent
- **WHEN** an authenticated owner sends `POST /api/listings/{listingId}/hide` for an already `ARCHIVED` listing
- **THEN** the system returns `204 No Content`
- **AND** the listing remains `ARCHIVED`

### Requirement: Landlord reactivates a hidden owned listing
The system SHALL provide `POST /api/listings/{listingId}/activate` that changes an owned hidden listing from status `ARCHIVED` back to status `PUBLISHED`.

#### Scenario: Successful reactivation
- **WHEN** an authenticated owner sends `POST /api/listings/{listingId}/activate` for an `ARCHIVED` listing
- **THEN** the system sets the listing status to `PUBLISHED`
- **AND** the system returns `204 No Content`

#### Scenario: Reactivated listing is visible again
- **WHEN** a listing has been reactivated to status `PUBLISHED`
- **THEN** public `GET /api/listings` includes that listing
- **AND** public listing availability lookup and new booking creation treat the listing as published

#### Scenario: Reactivation is idempotent
- **WHEN** an authenticated owner sends `POST /api/listings/{listingId}/activate` for an already `PUBLISHED` listing
- **THEN** the system returns `204 No Content`
- **AND** the listing remains `PUBLISHED`

### Requirement: Landlord deletes an owned listing when safe
The system SHALL provide `DELETE /api/listings/{listingId}` that removes an owned listing only when no booking references that listing.

#### Scenario: Successful delete without bookings
- **WHEN** an authenticated owner sends `DELETE /api/listings/{listingId}` for a listing that has no related bookings
- **THEN** the system deletes the listing and returns `204 No Content`
- **AND** subsequent public `GET /api/listings` and availability lookup do not include the listing

#### Scenario: Delete is blocked by booking history
- **WHEN** an authenticated owner sends `DELETE /api/listings/{listingId}` for a listing that is referenced by one or more bookings
- **THEN** the system returns `409 Conflict` with `ProblemDetail`
- **AND** the listing and related bookings remain unchanged

#### Scenario: Delete removes listing-only availability periods
- **WHEN** an authenticated owner deletes a listing that has no bookings but has listing unavailability periods
- **THEN** the listing is deleted
- **AND** listing unavailability periods for that listing are removed with the listing
