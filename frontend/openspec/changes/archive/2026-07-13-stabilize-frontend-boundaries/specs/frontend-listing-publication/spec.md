## ADDED Requirements

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
