## ADDED Requirements

### Requirement: Auth redirects preserve booking intent
The frontend SHALL preserve intended booking context when guests are routed to authentication from a protected booking action.

#### Scenario: Guest attempts booking confirmation
- **WHEN** a guest confirms a selected booking slot
- **THEN** the frontend SHALL route to login or registration with enough return context to resume the booking page after authentication

#### Scenario: Login completes with return context
- **WHEN** login succeeds after a booking-driven redirect
- **THEN** the frontend SHALL return to the original booking route so the user can submit or review the selected booking

#### Scenario: Registration completes with return context
- **WHEN** registration succeeds after a booking-driven redirect
- **THEN** the frontend SHALL preserve the path toward login and the original booking route where possible

#### Scenario: Return context is stale or missing
- **WHEN** stored router state no longer contains valid booking context
- **THEN** the frontend SHALL fall back to the booking page for the listing or the public catalog without throwing an application error
