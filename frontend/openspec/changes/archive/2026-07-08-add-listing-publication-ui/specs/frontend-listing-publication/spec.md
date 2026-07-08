## ADDED Requirements

### Requirement: Landlord can publish a listing
The frontend SHALL provide a landlord-facing page for publishing a commercial space listing through the backend listing publication API.

#### Scenario: Landlord opens publication form
- **WHEN** an authenticated landlord opens the publication route
- **THEN** the frontend SHALL show fields for title, space type, city, address, capacity, price per hour, description, and image URL

#### Scenario: Required fields are missing
- **WHEN** the landlord attempts to submit the form without required values
- **THEN** the frontend SHALL show client-side validation messages and SHALL NOT send the publication request

#### Scenario: Numeric values are invalid
- **WHEN** capacity is less than one or price per hour is not positive
- **THEN** the frontend SHALL show validation messages and SHALL NOT send the publication request

#### Scenario: Image URL is invalid
- **WHEN** image URL is present but is not an `http` or `https` URL
- **THEN** the frontend SHALL show a validation message and SHALL NOT send the publication request

#### Scenario: Publication request is pending
- **WHEN** the publication request is in progress
- **THEN** the frontend SHALL disable duplicate submission and show a visible submitting state

#### Scenario: Publication completes
- **WHEN** the backend returns the created listing
- **THEN** the frontend SHALL navigate to the created listing detail route or show a direct link to it

### Requirement: Publication form preserves usable state on recoverable errors
The frontend SHALL preserve entered publication form values when backend validation or role errors are recoverable.

#### Scenario: Backend validation error is shown
- **WHEN** the backend returns validation errors for publication
- **THEN** the frontend SHALL keep the user's entered values and show actionable messages

#### Scenario: Forbidden response is shown
- **WHEN** the backend returns `403`
- **THEN** the frontend SHALL show that only landlord accounts can publish listings
