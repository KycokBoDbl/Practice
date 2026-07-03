## Context

The frontend is a React + TypeScript + Vite application using React Router, CSS Modules, and an axios API client in `src/api`. Login and profile routes already exist, but they currently render placeholders and do not call the backend authentication API.

The backend already exposes the required auth contract:
- `POST /api/auth/register` accepts `{ role, legalName, taxId, email, password }` and returns `ProfileResponseDto`.
- `POST /api/auth/login` accepts `{ email, password }` and returns `{ accessToken, tokenType, expiresIn }`.
- `GET /api/auth/me` requires `Authorization: Bearer <accessToken>` and returns `ProfileResponseDto`.
- Errors use ProblemDetail, with validation errors exposed through an `errors` array containing `{ field, message }`.

This change is frontend-only. It must preserve catalog search, listing detail loading, booking page states, and booking calendar behavior.

## Goals / Non-Goals

**Goals:**
- Implement registration and login forms against the existing backend API.
- Store and use the JWT access token for authenticated requests.
- Load the current user profile through `/api/auth/me`.
- Protect `/profile` from unauthenticated access.
- Reflect authenticated and guest states in Header navigation.
- Present backend validation, `401`, and `409` errors in user-visible form states.

**Non-Goals:**
- No backend API changes.
- No refresh token, silent token renewal, or cookie-based session flow.
- No role-based feature gating beyond displaying the authenticated profile and logout.
- No changes to catalog filtering, listing detail loading, booking routes, or booking calendar behavior.

## Decisions

### Auth API module lives under `src/api`

Create a dedicated auth API module, for example `src/api/auth.ts`, to define DTO types and functions for `register`, `login`, and `getMe`.

Rationale: API communication already belongs in `src/api`, and keeping auth calls there avoids embedding backend details in route pages or layout components.

Alternatives considered:
- Calling axios directly from pages: rejected because it duplicates request/error handling and spreads backend DTO details across UI code.
- Generating a client from OpenAPI: deferred because the project currently uses hand-written API modules and this change should stay scoped.

### Token storage uses localStorage with expiry metadata

Persist the backend-issued access token in localStorage with token type and an absolute expiry timestamp derived from `expiresIn`.

Rationale: The backend only provides an access token and no refresh-token/cookie session flow. localStorage supports page reload persistence with minimal new infrastructure.

Alternatives considered:
- In-memory-only token: safer against XSS but would log users out on refresh and makes `/auth/me` bootstrapping less useful.
- HttpOnly cookie: requires backend API changes, which are out of scope.
- Refresh token storage: out of scope because the backend does not provide refresh tokens.

### Axios request interceptor attaches Authorization

The shared axios client should attach `Authorization: Bearer <accessToken>` when a non-expired token is present. Auth endpoints remain public, and public catalog/listing requests continue to work with or without the header.

Rationale: Centralizing the header prevents each protected API call from manually composing auth headers.

Alternatives considered:
- Passing headers per request: acceptable for one endpoint, but less maintainable as authenticated endpoints are added.

### Auth state is exposed through a provider/hook boundary

Add an auth state boundary, for example `AuthProvider` and `useAuth`, that owns token hydration, current-profile loading, login/register/logout actions, and `401` cleanup.

Rationale: Header, Login/Register pages, Profile page, and route protection need shared auth state. A provider/hook keeps this state out of layout-only components while remaining consistent with existing React patterns.

Alternatives considered:
- Page-local state only: rejected because Header and Profile need shared state.
- External state library: unnecessary for this scope.

### Registration does not imply a token unless login is performed

The backend registration endpoint returns a profile but not an access token. After successful registration, the frontend should either redirect the user to login with a success message or explicitly perform login using the submitted credentials if the UX requires immediate sign-in.

Decision for this change: redirect to login after successful registration and avoid implicit login.

Rationale: This follows the backend contract directly and avoids treating a profile response as an authenticated session.

Alternatives considered:
- Auto-login after registration: convenient, but it performs a second credential request and should be a deliberate UX choice. It can be added later without changing the backend contract.

### Profile route is protected at the route/page boundary

Protect `/profile` by checking auth state and redirecting unauthenticated users to `/login`, preserving the intended destination where practical.

Rationale: The router already centralizes route declarations, and Profile is the only protected route in scope.

Alternatives considered:
- Fetch profile directly inside `ProfilePage` without route protection: still possible internally, but route protection gives a predictable unauthenticated experience and avoids rendering protected UI before auth state is resolved.

### Error handling maps ProblemDetail into form state

Implement a small ProblemDetail parser that extracts:
- top-level `detail` for form-level messages;
- `errors[].field` and `errors[].message` for field-level validation feedback;
- status-specific handling for `401` and `409`.

Rationale: The backend returns standard ProblemDetail with field-level validation details. UI forms need stable, typed mapping without coupling directly to axios error internals.

Alternatives considered:
- Showing only raw `detail`: simpler, but loses backend validation fields.
- Duplicating parsing per form: rejected as unnecessary duplication.

## Risks / Trade-offs

- localStorage token persistence increases exposure if the app has an XSS issue -> Mitigation: keep token handling centralized, do not store password data, and clear tokens on logout/401.
- Access tokens expire after the backend TTL and no refresh token exists -> Mitigation: clear expired tokens locally, redirect to login, and preserve the current route where practical.
- Registration returns a profile but no token, which may surprise users -> Mitigation: show a clear success message on login after registration.
- Header now depends on auth state -> Mitigation: keep Header as navigation rendering only; it consumes auth state but must not own auth API calls or business flow.
- Backend validation messages are English -> Mitigation: display backend field/form messages reliably first; optional localization can be a future enhancement.
