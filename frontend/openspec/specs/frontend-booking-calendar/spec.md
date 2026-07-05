## Purpose

Defines frontend requirements for booking calendar structure and preserved booking behavior.
## Requirements
### Requirement: BookingCalendar is decomposed by responsibility
The frontend SHALL decompose booking calendar behavior into smaller units for availability loading, calendar/date calculations, selection state, business constants, and rendering.

#### Scenario: Availability data is loaded
- **WHEN** the calendar needs busy intervals for a listing and visible month
- **THEN** availability loading SHALL be handled by a dedicated hook or data boundary

#### Scenario: Calendar layout is calculated
- **WHEN** month days, slot status, or duration options are calculated
- **THEN** the calculation SHALL be handled by pure helpers or isolated logic outside the presentational rendering block

### Requirement: BookingCalendar visible behavior is preserved
The frontend SHALL preserve the current booking calendar UI behavior during decomposition.

#### Scenario: Preview mode is rendered
- **WHEN** `BookingCalendar` is rendered in preview mode
- **THEN** it SHALL continue to show availability information without enabling booking time selection

#### Scenario: Booking mode is rendered
- **WHEN** `BookingCalendar` is rendered in booking mode
- **THEN** it SHALL continue to allow date, time, and duration selection using the existing visible behavior

#### Scenario: Busy intervals are returned
- **WHEN** availability data marks time as busy
- **THEN** the calendar SHALL continue to reflect booked, partial, and available statuses consistently with current behavior

### Requirement: Booking selection state avoids effect-based correction where possible
The frontend SHALL avoid using effects for pure derived booking selection corrections when the same result can be derived or handled through explicit user actions.

#### Scenario: Selected slot becomes unavailable
- **WHEN** availability data makes the selected slot unavailable
- **THEN** the selection model SHALL resolve the displayed selected slot without lint-confirmed synchronous effect-state violations

#### Scenario: Duration exceeds available range
- **WHEN** selected duration exceeds the available range
- **THEN** the selection model SHALL resolve the displayed duration without lint-confirmed synchronous effect-state violations

### Requirement: Booking constants are isolated
The frontend SHALL isolate booking constants such as working hours and allowed durations from the main calendar rendering component.

#### Scenario: Booking rules are changed later
- **WHEN** working hours or allowed durations need to change
- **THEN** the change SHALL be localized to booking configuration or domain logic rather than scattered through rendering code

### Requirement: BookingCalendar exposes confirmable selection
The booking calendar SHALL expose enough selected slot data for a parent page to create a backend booking request.

#### Scenario: Available slot is selected
- **WHEN** the user selects an available date, start time, and duration in booking mode
- **THEN** the calendar SHALL expose `startAt`, `endAt`, and duration based on whole-hour local time strings

#### Scenario: No duration is available
- **WHEN** the selected slot has no valid available duration
- **THEN** the calendar SHALL disable confirmation and SHALL NOT emit a booking payload

#### Scenario: Preview mode is rendered
- **WHEN** the calendar is rendered in preview mode
- **THEN** it SHALL continue to show availability without exposing a booking confirmation action

### Requirement: BookingCalendar reflects submit state
The booking calendar SHALL support parent-controlled submission state without owning backend booking API calls.

#### Scenario: Booking request is submitting
- **WHEN** the parent page is submitting the selected booking request
- **THEN** the calendar confirmation control SHALL prevent duplicate submission and show a pending state

#### Scenario: Booking request fails
- **WHEN** the parent page receives a booking error
- **THEN** the calendar SHALL keep the current selection visible unless refreshed availability makes it unavailable

