## Purpose

Defines frontend requirements for legal entity registration, login, JWT-backed auth state, profile access, and auth-aware navigation.

## Requirements

### Requirement: Auth API module uses the existing backend contract
The frontend SHALL provide an auth API boundary for registration, login, and current-profile loading using the existing backend endpoints without changing request or response shapes.

#### Scenario: Registration request is submitted
- **WHEN** the frontend registers a user
- **THEN** it SHALL send `role`, `legalName`, `taxId`, `email`, and `password` to `POST /api/auth/register`

#### Scenario: Login request is submitted
- **WHEN** the frontend logs in a user
- **THEN** it SHALL send `email` and `password` to `POST /api/auth/login`

#### Scenario: Current profile is loaded
- **WHEN** the frontend loads the authenticated user profile
- **THEN** it SHALL call `GET /api/auth/me` and consume `userId`, `organizationId`, `role`, `legalName`, `taxId`, and `email`

### Requirement: Registration page supports legal entity signup
The frontend SHALL provide a registration page for creating accounts through the backend registration endpoint.

#### Scenario: User enters valid registration data
- **WHEN** the user submits valid `role`, `legalName`, `taxId`, `email`, and `password`
- **THEN** the frontend SHALL call the registration endpoint and show a successful path to login

#### Scenario: Backend returns validation errors
- **WHEN** the registration endpoint returns `400` with ProblemDetail field errors
- **THEN** the registration page SHALL show the relevant field-level validation messages where possible

#### Scenario: Backend returns registration conflict
- **WHEN** the registration endpoint returns `409`
- **THEN** the registration page SHALL show a form-level conflict message without clearing unrelated user input

### Requirement: Login page authenticates users
The frontend SHALL provide a login page that exchanges email and password for the backend access token.

#### Scenario: User enters valid credentials
- **WHEN** the user submits valid login credentials
- **THEN** the frontend SHALL store the returned access token metadata and proceed to the authenticated experience

#### Scenario: Backend rejects credentials
- **WHEN** the login endpoint returns `401`
- **THEN** the login page SHALL show an invalid-credentials message and SHALL NOT store an access token

#### Scenario: Login request is invalid
- **WHEN** the login endpoint returns `400` with ProblemDetail field errors
- **THEN** the login page SHALL show the relevant field-level validation messages where possible

### Requirement: Access token is stored and applied consistently
The frontend SHALL persist the backend-issued JWT access token and apply it to authenticated API requests.

#### Scenario: Token is received from login
- **WHEN** the login endpoint returns `accessToken`, `tokenType`, and `expiresIn`
- **THEN** the frontend SHALL store the token with enough metadata to determine local expiry

#### Scenario: Authenticated request is sent
- **WHEN** a non-expired access token is available
- **THEN** the frontend SHALL send `Authorization: Bearer <accessToken>` for authenticated API requests

#### Scenario: Token is expired locally
- **WHEN** the stored token is past its local expiry
- **THEN** the frontend SHALL clear the stored token and treat the user as unauthenticated

### Requirement: Current user is loaded from auth state
The frontend SHALL load and expose the current authenticated user's profile through a shared auth state boundary.

#### Scenario: Application starts with a stored token
- **WHEN** the application initializes and a non-expired token exists
- **THEN** the frontend SHALL request `/api/auth/me` and expose the returned profile to authenticated UI

#### Scenario: Current profile request is unauthorized
- **WHEN** `/api/auth/me` returns `401`
- **THEN** the frontend SHALL clear the stored token and expose an unauthenticated state

#### Scenario: User logs out
- **WHEN** the user chooses logout
- **THEN** the frontend SHALL clear token and profile state and return to guest navigation

### Requirement: Profile page is protected
The frontend SHALL prevent unauthenticated users from viewing the Profile page.

#### Scenario: Guest opens profile route
- **WHEN** an unauthenticated user navigates to `/profile`
- **THEN** the frontend SHALL redirect or navigate them to the login page

#### Scenario: Authenticated user opens profile route
- **WHEN** an authenticated user navigates to `/profile`
- **THEN** the frontend SHALL show the current profile data returned by `/api/auth/me`

#### Scenario: Auth state is still loading
- **WHEN** the frontend is determining whether a stored token is valid
- **THEN** the Profile page SHALL avoid briefly showing unauthenticated placeholder content as the final state

### Requirement: Header reflects authentication state
The frontend SHALL update Header navigation based on whether the user is authenticated while preserving navigation-only responsibilities.

#### Scenario: Guest views the header
- **WHEN** no authenticated user is present
- **THEN** Header SHALL show navigation for login and registration

#### Scenario: Authenticated user views the header
- **WHEN** an authenticated user is present
- **THEN** Header SHALL show navigation for profile and logout

#### Scenario: Logout is triggered from the header
- **WHEN** the user activates logout in Header
- **THEN** Header SHALL invoke shared auth logout behavior without owning token storage or auth API calls

### Requirement: Existing catalog and booking behavior is preserved
The frontend SHALL add authentication without changing existing public catalog, listing detail, booking, or booking calendar behavior.

#### Scenario: Guest opens the catalog
- **WHEN** an unauthenticated user opens the catalog
- **THEN** catalog listing loading and search/filter behavior SHALL continue to work without requiring authentication

#### Scenario: Guest opens listing detail or availability
- **WHEN** an unauthenticated user opens listing detail or booking availability views
- **THEN** existing listing detail and booking calendar preview/booking mode behavior SHALL remain available as before
