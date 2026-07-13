## ADDED Requirements

### Requirement: Availability loading ignores stale responses
The frontend SHALL protect booking calendar availability state from stale responses when listing id, visible month, or refresh key changes.

#### Scenario: Visible month changes quickly
- **WHEN** multiple availability requests are started for different visible months
- **THEN** only the latest relevant response SHALL update the calendar availability state

#### Scenario: Listing changes while request is pending
- **WHEN** the listing id changes before a previous availability request completes
- **THEN** the previous request SHALL NOT overwrite availability state for the new listing

#### Scenario: Component unmounts while request is pending
- **WHEN** the booking calendar unmounts before availability loading completes
- **THEN** the pending response SHALL NOT update React state after unmount

### Requirement: Availability errors are represented separately
The frontend SHALL represent availability loading errors separately from successful empty busy-interval results.

#### Scenario: Availability request succeeds with no busy intervals
- **WHEN** the backend returns an empty `busyIntervals` array
- **THEN** the calendar SHALL treat the month as successfully loaded with no busy intervals

#### Scenario: Availability request fails
- **WHEN** availability loading fails
- **THEN** the calendar SHALL expose an availability error state without presenting it as a successful empty busy interval list

#### Scenario: Availability is retried
- **WHEN** availability is reloaded after a previous error
- **THEN** the latest successful response SHALL clear the availability error state for that listing and month
