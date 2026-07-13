## ADDED Requirements

### Requirement: Landlord listing management is decomposed by responsibility
The frontend SHALL decompose landlord listing management implementation into stable responsibilities without changing the management workflow.

#### Scenario: Owned listings are loaded
- **WHEN** the landlord management page loads owned listings
- **THEN** data loading, loading/error/empty state ownership, and mutation refresh behavior MAY be handled by a focused hook or route-level data boundary

#### Scenario: Edit form is used
- **WHEN** the landlord opens, edits, validates, submits, or closes the listing edit form
- **THEN** form state, validation, and request payload construction SHALL be separated from unrelated list rendering where practical

#### Scenario: Listing action is triggered
- **WHEN** the landlord hides, activates, or deletes a listing
- **THEN** action handling SHALL preserve existing confirmation, success, conflict, forbidden, and not-found behavior

#### Scenario: Management view is rendered
- **WHEN** management cards, filters, summaries, or edit form sections are extracted
- **THEN** extracted presentational components SHALL NOT own backend API calls
