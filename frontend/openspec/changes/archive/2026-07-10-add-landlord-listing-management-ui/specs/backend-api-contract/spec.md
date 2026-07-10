## ADDED Requirements

### Requirement: Frontend consumes existing listing management API
The frontend SHALL consume the existing backend listing management contract for owner-only edit, hide, activate, and delete operations without requiring backend changes in this change.

#### Scenario: Listing edit is submitted
- **WHEN** the frontend updates an owned listing
- **THEN** it SHALL call `PUT /api/listings/{listingId}` with the backend listing update request shape
- **AND** it SHALL consume the returned `ListingResponseDto`

#### Scenario: Listing is hidden
- **WHEN** the frontend hides an owned listing
- **THEN** it SHALL call `POST /api/listings/{listingId}/hide`
- **AND** it SHALL treat successful `204` as confirmation that the listing is no longer active

#### Scenario: Listing is activated
- **WHEN** the frontend publishes a hidden listing again
- **THEN** it SHALL call `POST /api/listings/{listingId}/activate`
- **AND** it SHALL treat successful `204` as confirmation that the listing is active again

#### Scenario: Listing is deleted
- **WHEN** the frontend permanently deletes an owned listing
- **THEN** it SHALL call `DELETE /api/listings/{listingId}`
- **AND** it SHALL handle `409 Conflict` as a recoverable state where deletion is blocked by booking history

#### Scenario: Owner list contract is unavailable
- **WHEN** the existing backend contract does not provide owned listing discovery including hidden listings
- **THEN** frontend implementation SHALL stop before adding backend changes
- **AND** the missing backend contract SHALL be reported for explicit user approval
