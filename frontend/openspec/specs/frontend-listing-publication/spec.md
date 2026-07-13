## Purpose

Defines frontend requirements for landlord listing publication through the existing backend listings API.

## Requirements

### Requirement: Landlord can publish a listing
The frontend SHALL provide a landlord-facing page for publishing a commercial space listing through the backend listing publication API.

#### Scenario: Landlord opens publication form
- **WHEN** an authenticated landlord opens the publication route
- **THEN** the frontend SHALL show fields for title, space type, city, address, capacity, price per hour, description, and image URL

#### Scenario: Required fields are missing
- **WHEN** the landlord attempts to submit the form without required values
- **THEN** the frontend SHALL show client-side validation messages
- **AND** the frontend SHALL NOT send the publication request

#### Scenario: Numeric values are invalid
- **WHEN** capacity is less than one or price per hour is not positive
- **THEN** the frontend SHALL show validation messages
- **AND** the frontend SHALL NOT send the publication request

#### Scenario: Image URL is invalid
- **WHEN** image URL is present but is not an `http` or `https` URL
- **THEN** the frontend SHALL show a validation message
- **AND** the frontend SHALL NOT send the publication request

#### Scenario: Publication request is pending
- **WHEN** the publication request is in progress
- **THEN** the frontend SHALL prevent duplicate submission
- **AND** the frontend SHALL show a visible submitting state

#### Scenario: Publication completes
- **WHEN** the backend returns the created listing
- **THEN** the frontend SHALL surface a direct path to the created listing detail route

### Requirement: Publication form preserves state on recoverable errors
The frontend SHALL preserve entered form values when recoverable backend publication errors occur.

#### Scenario: Backend validation error is shown
- **WHEN** the backend returns `400` for publication
- **THEN** the frontend SHALL keep the entered values
- **AND** the frontend SHALL map validation details into field-level or form-level messages where possible

#### Scenario: Forbidden response is shown
- **WHEN** the backend returns `403`
- **THEN** the frontend SHALL show that only landlord accounts can publish listings

### Requirement: Listing publication is decomposed by responsibility
The frontend SHALL decompose listing publication implementation into stable responsibilities without changing the publication workflow.

#### Scenario: Publication form is edited
- **WHEN** the landlord edits publication form fields, helper selections, or preview data
- **THEN** form state and preview derivation SHALL remain behaviorally equivalent after decomposition

#### Scenario: Publication validation runs
- **WHEN** the landlord submits invalid publication input
- **THEN** validation helpers SHALL preserve existing client-side validation, first-error focus behavior, and prevention of backend submission

#### Scenario: Publication request is submitted
- **WHEN** valid publication data is submitted
- **THEN** the frontend SHALL continue to call the existing listing publication API contract without changing request shape

#### Scenario: Publication sections are extracted
- **WHEN** form sections, preview, helper chips, or status panels are extracted into smaller components
- **THEN** those presentational components SHALL NOT own backend API calls
