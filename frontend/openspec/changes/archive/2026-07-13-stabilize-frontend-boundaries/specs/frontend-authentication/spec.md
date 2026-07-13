## ADDED Requirements

### Requirement: Protected route redirects use returnTo consistently
The frontend SHALL use a single `returnTo` redirect-state contract when unauthenticated users are sent to authentication from protected routes.

#### Scenario: Guest opens a protected route
- **WHEN** an unauthenticated user opens a protected route such as `/profile`, `/bookings`, `/my-listings`, or `/spaces/new`
- **THEN** the route guard SHALL navigate to login with a sanitized `returnTo` value for the originally requested location

#### Scenario: Login succeeds after protected-route redirect
- **WHEN** login succeeds and a valid `returnTo` value is present
- **THEN** the frontend SHALL navigate back to that route instead of falling back to the default authenticated destination

#### Scenario: Registration is chosen from redirected login
- **WHEN** a redirected guest moves from login to registration
- **THEN** the frontend SHALL preserve the same sanitized `returnTo` value through registration and back to login

#### Scenario: Return target is unsafe
- **WHEN** redirect state contains an external, protocol-relative, login, register, or otherwise unsafe target
- **THEN** the frontend SHALL ignore it and use the existing safe fallback destination
