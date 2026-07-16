## Purpose

Defines frontend requirements for catalog search ownership, URL query behavior, and header boundaries.

## Requirements

### Requirement: Header remains navigation-only
The frontend SHALL keep `Header` responsible only for brand/navigation rendering and route navigation links.

#### Scenario: Header renders on non-catalog routes
- **WHEN** the user opens a non-catalog route such as login or profile
- **THEN** `Header` SHALL NOT load listing data or initialize catalog filter business state

#### Scenario: Header navigation remains available
- **WHEN** the user views any route under the main layout
- **THEN** `Header` SHALL continue to show the existing brand and navigation links

### Requirement: Catalog owns search and filters
The frontend SHALL place catalog search/filter parsing, draft state, option derivation, and submission behavior in a catalog-owned component, hook, or utility outside `Header`.

#### Scenario: User submits catalog search
- **WHEN** the user submits a search query or filter values from the catalog search boundary
- **THEN** the frontend SHALL update the same URL query parameters currently used by the catalog

#### Scenario: User resets filters
- **WHEN** the user resets catalog filters
- **THEN** the frontend SHALL clear catalog search/filter query parameters and show the unfiltered catalog

### Requirement: Catalog filtering behavior is preserved
The frontend SHALL preserve existing catalog filtering semantics for search text, city, minimum capacity, maximum capacity, minimum price, and maximum price.

#### Scenario: Query parameters are present
- **WHEN** the catalog route is opened with existing filter query parameters
- **THEN** the catalog SHALL derive the displayed listing set from those query parameters

#### Scenario: No filters are present
- **WHEN** the catalog route is opened without search/filter query parameters
- **THEN** the catalog SHALL show all loaded listings

### Requirement: Catalog filter logic is testable outside rendering
The frontend SHALL expose catalog query parsing and listing filtering logic in a form that can be tested without rendering the full catalog page.

#### Scenario: Filter utility receives listings and query state
- **WHEN** filter logic receives listing data and parsed query state
- **THEN** it SHALL return the same filtered listing set that the catalog page renders

### Requirement: Catalog supports AI-assisted listing search
The frontend SHALL provide a catalog-owned AI-assisted search flow that submits a natural-language prompt to the backend AI listing search endpoint and renders the returned listings without replacing existing deterministic filters.

#### Scenario: User submits AI search prompt
- **WHEN** the user submits a non-empty AI search prompt from the catalog page
- **THEN** the frontend SHALL call the shared listing API boundary for `POST /api/listings/ai-search`
- **AND** the frontend SHALL render the returned listings using the existing catalog listing presentation

#### Scenario: AI search is active
- **WHEN** AI search results are being displayed
- **THEN** the frontend SHALL show a visible active AI-search state containing the submitted prompt or equivalent user-facing context
- **AND** the frontend SHALL provide a way to clear AI search and return to the ordinary catalog/filter view

#### Scenario: Ordinary filters remain available
- **WHEN** AI search is not active
- **THEN** the frontend SHALL preserve the existing catalog search text, city, capacity, and price filter behavior

#### Scenario: AI search is cleared
- **WHEN** the user clears AI search
- **THEN** the frontend SHALL stop displaying AI search results
- **AND** the frontend SHALL derive the visible listing set from the ordinary catalog listings and URL filter state

### Requirement: Catalog handles AI search states safely
The frontend SHALL expose clear loading, empty, validation-error, and service-error states for AI-assisted search while keeping the ordinary catalog usable.

#### Scenario: AI search is loading
- **WHEN** an AI search request is in progress
- **THEN** the frontend SHALL show a loading state for AI search
- **AND** the frontend SHALL prevent duplicate submissions for the same in-flight request

#### Scenario: AI search returns no listings
- **WHEN** the backend returns an empty listing array for AI search
- **THEN** the frontend SHALL show an empty state specific to the submitted AI prompt
- **AND** the frontend SHALL provide a way to clear AI search

#### Scenario: AI search request is invalid
- **WHEN** the backend rejects AI search with `400`
- **THEN** the frontend SHALL show a recoverable prompt/input error
- **AND** the frontend SHALL NOT clear ordinary catalog listings or filters

#### Scenario: AI search service is unavailable
- **WHEN** the backend rejects AI search with `502`
- **THEN** the frontend SHALL show a recoverable message that AI search is temporarily unavailable
- **AND** the frontend SHALL keep the ordinary catalog available

### Requirement: Catalog AI search respects prompt constraints
The frontend SHALL enforce the backend AI search prompt contract before submission where practical.

#### Scenario: Prompt is blank
- **WHEN** the user attempts to submit an empty or whitespace-only AI search prompt
- **THEN** the frontend SHALL show a validation message
- **AND** the frontend SHALL NOT send the AI search request

#### Scenario: Prompt is too long
- **WHEN** the AI search prompt exceeds the backend maximum of `1000` characters
- **THEN** the frontend SHALL prevent or reject submission with a user-facing validation message

### Requirement: Catalog touched text remains readable
The frontend SHALL keep user-visible strings in catalog/search files touched by this change readable in Russian and free of mojibake.

#### Scenario: Catalog text is rendered
- **WHEN** the user opens the catalog and uses ordinary or AI-assisted search
- **THEN** labels, buttons, empty states, loading states, and errors in touched catalog/search files SHALL display readable Russian text

#### Scenario: Mojibake check is available
- **WHEN** the repository's lightweight mojibake check is run
- **THEN** touched frontend catalog/search files SHALL NOT introduce characteristic mojibake patterns
