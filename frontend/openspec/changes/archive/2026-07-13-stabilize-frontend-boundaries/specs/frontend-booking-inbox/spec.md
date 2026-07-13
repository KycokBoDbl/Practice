## ADDED Requirements

### Requirement: Booking inbox polling is isolated
The frontend SHALL isolate booking inbox polling and refresh orchestration from the booking inbox page rendering component.

#### Scenario: Inbox page opens
- **WHEN** an authenticated user opens the booking inbox page
- **THEN** a focused hook or data boundary SHALL load inbox data and expose items, loading state, error state, refreshing state, pending state, and refresh behavior to the page

#### Scenario: Polling interval fires
- **WHEN** the inbox polling interval fires while no inbox request is pending
- **THEN** the hook SHALL reload inbox data and update visible status data from the backend response

#### Scenario: Request is already pending
- **WHEN** a manual refresh or polling tick occurs while an inbox request is already pending
- **THEN** the frontend SHALL avoid starting an overlapping inbox request

#### Scenario: Inbox page unmounts
- **WHEN** the user leaves the booking inbox page
- **THEN** the frontend SHALL stop polling and SHALL NOT update inbox state after unmount

#### Scenario: Hook dependencies are validated
- **WHEN** lint is run
- **THEN** inbox polling implementation SHALL pass `react-hooks/exhaustive-deps` without a local suppression around the polling effect

### Requirement: Booking inbox presentation is separated from data orchestration
The frontend SHALL separate booking inbox data orchestration from status filtering and card/list presentation where that reduces component responsibility.

#### Scenario: Inbox items are rendered
- **WHEN** inbox data is loaded
- **THEN** presentational components or pure helpers SHALL render status labels, summary counts, filters, and booking cards without owning API calls

#### Scenario: Inbox filters are used
- **WHEN** the user switches inbox filters
- **THEN** the existing all, action-required, confirmed, active, and completed views SHALL remain behaviorally equivalent
