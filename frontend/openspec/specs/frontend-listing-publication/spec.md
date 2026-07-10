## Purpose

Defines frontend requirements for landlord listing publication through the existing backend listings API.

## Requirements

### Requirement: Landlord can publish a listing
The frontend SHALL provide a landlord-facing editable listing preview for publishing a commercial space listing through the backend listing publication API.

#### Scenario: Landlord opens publication form
- **WHEN** an authenticated landlord opens the publication route
- **THEN** the frontend SHALL show editable controls for title, space type, city, address, capacity, price per hour, description, image URL, and selected helper chips
- **AND** the page SHALL present the entered values as a listing preview rather than only as a plain form

#### Scenario: Required fields are missing
- **WHEN** the landlord attempts to submit the form without required values
- **THEN** the frontend SHALL show client-side validation messages
- **AND** the frontend SHALL scroll to the first invalid field
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

### Requirement: Publication preview remains compact and operational
The publication page SHALL use the preview model to improve confidence before submission without hiding required input controls.

#### Scenario: Preview updates while editing
- **WHEN** the landlord changes listing fields
- **THEN** the visible preview SHALL update from the current local form state

#### Scenario: Image is missing
- **WHEN** no image URL is provided
- **THEN** the preview SHALL show the existing placeholder treatment rather than a broken image

#### Scenario: Owner area is shown without owner data
- **WHEN** the preview includes the future owner information area
- **THEN** the frontend SHALL avoid displaying internal owner ids as a user-facing legal entity name

### Requirement: Publication form preserves state on recoverable errors
The frontend SHALL preserve entered form values when recoverable backend publication errors occur.

#### Scenario: Backend validation error is shown
- **WHEN** the backend returns `400` for publication
- **THEN** the frontend SHALL keep the entered values
- **AND** the frontend SHALL map validation details into field-level or form-level messages where possible

#### Scenario: Forbidden response is shown
- **WHEN** the backend returns `403`
- **THEN** the frontend SHALL show that only landlord accounts can publish listings
