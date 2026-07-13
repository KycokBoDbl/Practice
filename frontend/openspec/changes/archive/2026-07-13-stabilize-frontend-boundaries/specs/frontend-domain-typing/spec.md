## ADDED Requirements

### Requirement: Booking inbox nullable fields match backend contract
The frontend SHALL model booking inbox organization-name fields according to backend nullability.

#### Scenario: Tenant organization name is absent
- **WHEN** a booking inbox item has no tenant organization name from the backend
- **THEN** TypeScript SHALL allow the value to be `null`
- **AND** UI code SHALL render a safe fallback rather than assuming a non-null string

#### Scenario: Landlord organization name is absent
- **WHEN** a booking inbox item has no landlord organization name from the backend
- **THEN** TypeScript SHALL allow the value to be `null`
- **AND** UI code SHALL render a safe fallback rather than assuming a non-null string

#### Scenario: Booking DTO remains backend-compatible
- **WHEN** booking inbox data is consumed from `GET /api/bookings`
- **THEN** frontend type changes SHALL NOT require a backend response shape change
