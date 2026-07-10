## Purpose

Defines frontend requirements for landlord listing management over owned listings.

## Requirements

### Requirement: Landlord can open owned listing management
The frontend SHALL provide a protected landlord-only page for managing listings owned by the current organization.

#### Scenario: Landlord opens my listings
- **WHEN** an authenticated user with role `LANDLORD` opens the owned listing management route
- **THEN** the frontend SHALL load owned listing data through the shared listing API boundary
- **AND** the frontend SHALL render the management page inside the main application layout

#### Scenario: Guest opens my listings
- **WHEN** an unauthenticated user opens the owned listing management route
- **THEN** the frontend SHALL route the user through the existing authentication flow before protected listing data is loaded

#### Scenario: Tenant opens my listings
- **WHEN** an authenticated user with role `TENANT` opens the owned listing management route
- **THEN** the frontend SHALL show a safe forbidden or unavailable state
- **AND** the frontend SHALL NOT load landlord-only management data

### Requirement: Landlord listing management is grouped by lifecycle state
The frontend SHALL group owned listings into compact status tabs that help landlords distinguish active and hidden spaces.

#### Scenario: Active tab is selected
- **WHEN** the landlord selects the active listings tab
- **THEN** the frontend SHALL show owned listings that are currently published or otherwise catalog-visible according to backend data

#### Scenario: Hidden tab is selected
- **WHEN** the landlord selects the hidden listings tab
- **THEN** the frontend SHALL show owned listings that are hidden or archived according to backend data

#### Scenario: All tab is selected
- **WHEN** the landlord selects the all listings tab
- **THEN** the frontend SHALL show all owned listings returned by the backend management contract

#### Scenario: No listings match selected tab
- **WHEN** the selected tab has no matching owned listings
- **THEN** the frontend SHALL show an empty state specific to that tab rather than a load error

### Requirement: Landlord can edit an owned listing
The frontend SHALL allow landlords to edit mutable listing fields through the existing backend update endpoint.

#### Scenario: Landlord opens edit flow
- **WHEN** the landlord chooses to edit an owned listing
- **THEN** the frontend SHALL populate editable fields from the selected listing
- **AND** the frontend SHALL preserve the listing owner and lifecycle state as server-controlled data

#### Scenario: Edit validation fails
- **WHEN** required edit fields are missing or numeric values are invalid
- **THEN** the frontend SHALL show validation messages
- **AND** the frontend SHALL NOT send the update request

#### Scenario: Edit succeeds
- **WHEN** the backend returns the updated listing
- **THEN** the frontend SHALL update the visible listing data from the backend response
- **AND** the frontend SHALL keep the landlord in the management workflow

#### Scenario: Edit request is rejected
- **WHEN** the backend returns `400`, `403`, or `404` for an edit request
- **THEN** the frontend SHALL show a recoverable message without clearing the management page state

### Requirement: Landlord can hide and reactivate an owned listing
The frontend SHALL expose hide and publish-again actions for owned listings when those actions match the listing state.

#### Scenario: Published listing is hidden
- **WHEN** the landlord hides an active owned listing
- **THEN** the frontend SHALL call the backend hide endpoint
- **AND** the listing SHALL move out of the active tab after the backend confirms success

#### Scenario: Hidden listing is published again
- **WHEN** the landlord publishes a hidden owned listing again
- **THEN** the frontend SHALL call the backend activate endpoint
- **AND** the listing SHALL move back to the active tab after the backend confirms success

#### Scenario: Lifecycle action fails
- **WHEN** hide or activate returns `401`, `403`, or `404`
- **THEN** the frontend SHALL show a role-safe or not-found message without removing unrelated listings from the page

### Requirement: Landlord can permanently delete safe listings
The frontend SHALL support permanent deletion only with explicit confirmation and backend conflict handling.

#### Scenario: Delete is requested
- **WHEN** the landlord chooses to delete an owned listing
- **THEN** the frontend SHALL require explicit confirmation before sending the delete request

#### Scenario: Delete succeeds
- **WHEN** the backend returns successful deletion
- **THEN** the frontend SHALL remove the listing from active, hidden, and all management views

#### Scenario: Delete is blocked by booking history
- **WHEN** the backend returns `409 Conflict` for deletion
- **THEN** the frontend SHALL explain that booking history prevents permanent deletion
- **AND** the frontend SHALL keep the listing visible and suggest hiding it when applicable

#### Scenario: Delete target is unavailable
- **WHEN** the backend returns `403` or `404` for deletion
- **THEN** the frontend SHALL show a safe unavailable state without exposing unrelated owner data

### Requirement: Owner display placeholder is prepared
The frontend SHALL reserve a stable UI location for future listing owner organization display without depending on an unavailable owner-name field.

#### Scenario: Owner name is unavailable
- **WHEN** listing data does not include a human-readable owner organization name
- **THEN** the frontend SHALL NOT show internal owner identifiers as user-facing owner names
- **AND** the listing UI SHALL remain visually stable for a future owner block
