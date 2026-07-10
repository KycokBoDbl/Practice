## ADDED Requirements

### Requirement: Frontend exposes landlord listing management navigation
The frontend SHALL expose owned listing management navigation to authenticated landlords without showing it to guests or tenants.

#### Scenario: Landlord sees my listings entry
- **WHEN** an authenticated user with role `LANDLORD` sees the main header
- **THEN** the frontend SHALL show a navigation entry labeled "Мои объявления"

#### Scenario: Tenant or guest sees header
- **WHEN** the current user is a tenant or unauthenticated guest
- **THEN** the frontend SHALL NOT show landlord listing management navigation

### Requirement: Frontend protects landlord listing management route
The frontend SHALL protect the owned listing management route with existing authentication and role-aware access handling.

#### Scenario: Guest opens management route directly
- **WHEN** a guest navigates to the owned listing management route directly
- **THEN** the frontend SHALL route the user through the existing login flow

#### Scenario: Tenant opens management route directly
- **WHEN** an authenticated tenant navigates to the owned listing management route
- **THEN** the frontend SHALL show a safe forbidden or unavailable state

#### Scenario: Landlord opens management route directly
- **WHEN** an authenticated landlord navigates to the owned listing management route
- **THEN** the frontend SHALL render the listing management workflow
