## ADDED Requirements

### Requirement: Frontend user-visible text remains readable
The frontend SHALL keep user-visible text literals readable in their intended language and SHALL guard against characteristic mojibake sequences in source files.

#### Scenario: Source text is checked
- **WHEN** frontend verification is run
- **THEN** it SHALL include a lightweight check for characteristic mojibake patterns in user-facing frontend source files if this can be implemented with the existing Node/npm toolchain

#### Scenario: Mojibake text is found
- **WHEN** the check detects characteristic mojibake patterns in user-facing frontend source
- **THEN** verification SHALL fail with file locations that can be corrected before merge

#### Scenario: No extra infrastructure is needed
- **WHEN** the mojibake check is implemented
- **THEN** it SHALL NOT require a new service, external dependency, or backend change

### Requirement: Frontend refactoring preserves behavior
The frontend SHALL allow decomposition of oversized route components only when the existing user-visible workflow is preserved.

#### Scenario: Route component is decomposed
- **WHEN** fetching, filtering, validation, mutation handling, formatting, or rendering is extracted from a large page
- **THEN** the existing route path, API calls, visible states, and user actions SHALL remain equivalent unless separately specified

#### Scenario: Pure helpers are extracted
- **WHEN** formatting, validation, status classification, or description parsing is moved out of a component
- **THEN** the extracted helper SHALL preserve current inputs and outputs for existing workflows

#### Scenario: Presentational components are extracted
- **WHEN** repeated or large JSX sections are moved into presentational components
- **THEN** business data loading and mutation ownership SHALL remain in route-level hooks or page orchestration
