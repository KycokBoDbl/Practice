## ADDED Requirements

### Requirement: Booking inbox refreshes statuses automatically
The frontend SHALL keep the booking inbox status list fresh through conservative polling while preserving manual refresh behavior.

#### Scenario: Inbox is open
- **WHEN** an authenticated user keeps the booking inbox page open
- **THEN** the frontend SHALL periodically reload inbox data from the backend
- **AND** visible booking statuses SHALL be updated from the backend response

#### Scenario: Previous request is still pending
- **WHEN** a polling interval occurs while an inbox request is already pending
- **THEN** the frontend SHALL NOT start an overlapping inbox request

#### Scenario: Inbox is closed
- **WHEN** the user leaves the booking inbox page
- **THEN** the frontend SHALL stop polling for inbox data

#### Scenario: Manual refresh is used
- **WHEN** the user triggers manual refresh
- **THEN** the frontend SHALL reload inbox data immediately without disabling future polling

### Requirement: Booking inbox exposes status-focused tabs
The frontend SHALL provide compact booking inbox tabs that make important booking lifecycle states easy to find.

#### Scenario: Action-required tab is selected
- **WHEN** the user selects the action-required tab
- **THEN** the frontend SHALL show bookings requiring the current role's next action

#### Scenario: Confirmed tab is selected
- **WHEN** the user selects the confirmed tab
- **THEN** the frontend SHALL show bookings with status `CONFIRMED`

#### Scenario: Active tab is selected
- **WHEN** the user selects the active tab
- **THEN** the frontend SHALL show bookings with status `IN_PROGRESS`

#### Scenario: Completed tab is selected
- **WHEN** the user selects the completed tab
- **THEN** the frontend SHALL show bookings with status `COMPLETED`

#### Scenario: All bookings tab is selected
- **WHEN** the user selects the all bookings tab
- **THEN** the frontend SHALL show all inbox items returned by the backend for the current participant
