## ADDED Requirements

### Requirement: Header exposes landlord publication navigation
The frontend SHALL expose listing publication navigation to authenticated landlords without showing it to guests or tenants.

#### Scenario: Landlord views header
- **WHEN** an authenticated user with role `LANDLORD` sees the Header
- **THEN** the Header SHALL include a navigation entry to the listing publication route

#### Scenario: Tenant views header
- **WHEN** an authenticated user with role `TENANT` sees the Header
- **THEN** the Header SHALL NOT show listing publication navigation

#### Scenario: Guest views header
- **WHEN** a guest sees the Header
- **THEN** the Header SHALL NOT show listing publication navigation

### Requirement: Listing publication route is protected
The frontend SHALL protect the listing publication route with existing authentication behavior and role-aware access handling.

#### Scenario: Guest opens publication route
- **WHEN** a guest navigates directly to the listing publication route
- **THEN** the frontend SHALL route the user through the existing login flow before protected publication data or actions are available

#### Scenario: Tenant opens publication route
- **WHEN** an authenticated tenant navigates directly to the listing publication route
- **THEN** the frontend SHALL show a clear unavailable or forbidden state and SHALL NOT submit publication requests

#### Scenario: Landlord opens publication route
- **WHEN** an authenticated landlord navigates to the listing publication route
- **THEN** the frontend SHALL render the publication form inside the main application layout
