## ADDED Requirements

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
