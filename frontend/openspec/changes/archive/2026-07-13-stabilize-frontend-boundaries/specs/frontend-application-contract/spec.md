## ADDED Requirements

### Requirement: Application routes preserve behavior during stabilization
The frontend SHALL preserve existing application routes, navigation labels, and user workflows while stabilizing internal implementation boundaries.

#### Scenario: Existing route is opened
- **WHEN** a user opens any existing public or protected frontend route
- **THEN** the route path and intended page workflow SHALL remain available after stabilization

#### Scenario: User-visible text is corrected
- **WHEN** mojibake user-facing strings are corrected
- **THEN** the resulting text SHALL convey the same existing labels, messages, and actions in readable form rather than introducing redesigned copy

#### Scenario: Backend-backed workflow is used
- **WHEN** catalog, listing publication, listing management, booking calendar, booking inbox, or booking detail workflows call the backend
- **THEN** they SHALL continue to use the existing frontend API functions and backend API contracts unless a separate OpenSpec change changes that contract

### Requirement: Stabilization excludes backend-dependent listing detail redesign
The frontend SHALL NOT introduce a new listing-detail API assumption as part of this stabilization change.

#### Scenario: Listing detail is loaded
- **WHEN** listing detail or booking routes resolve a listing by id
- **THEN** this change SHALL NOT require or assume `GET /api/listings/{listingId}`

#### Scenario: Listing API boundary is considered
- **WHEN** implementation touches frontend route decomposition
- **THEN** it SHALL NOT change the current `getListing()` behavior or `src/api/listings.ts` as part of this change
