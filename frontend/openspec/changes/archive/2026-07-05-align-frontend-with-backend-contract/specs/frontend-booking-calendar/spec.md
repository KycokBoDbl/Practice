## ADDED Requirements

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
