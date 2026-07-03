## 1. Auth Contract and Storage

- [x] 1.1 Add frontend auth DTO types for register, login, token response, profile response, role, and ProblemDetail errors.
- [x] 1.2 Add an auth API module in `src/api` for `register`, `login`, and `getMe` using the existing backend endpoints.
- [x] 1.3 Add token storage helpers that persist `accessToken`, `tokenType`, and local expiry metadata without storing passwords.
- [x] 1.4 Update the shared API client to attach `Authorization: Bearer <accessToken>` when a non-expired token is available.
- [x] 1.5 Ensure expired or malformed stored token data is cleared and treated as unauthenticated.

## 2. Auth State Boundary

- [x] 2.1 Add a shared auth provider/hook boundary that exposes profile, auth loading state, login, register, refresh current user, and logout actions.
- [x] 2.2 Hydrate auth state on app startup by reading stored token metadata and calling `/api/auth/me` when the token is still valid.
- [x] 2.3 Clear token and profile state when `/api/auth/me` or another authenticated request returns `401`.
- [x] 2.4 Wrap the router/application tree with the auth provider without changing existing catalog or booking route behavior.

## 3. Registration Flow

- [x] 3.1 Add a registration route and page with fields for `role`, `legalName`, `taxId`, `email`, and `password`.
- [x] 3.2 Submit valid registration form data to `POST /api/auth/register` through the auth state/API boundary.
- [x] 3.3 Show backend `400` field validation errors next to relevant registration fields where possible.
- [x] 3.4 Show backend `409` registration conflict as a form-level error without clearing unrelated input.
- [x] 3.5 After successful registration, navigate to login and show a clear success path without treating the registration response as an authenticated session.

## 4. Login Flow

- [x] 4.1 Replace the login placeholder with a login form for `email` and `password`.
- [x] 4.2 Submit credentials to `POST /api/auth/login` and persist returned token metadata on success.
- [x] 4.3 Load the current profile through `/api/auth/me` after successful login.
- [x] 4.4 Show backend `401` invalid-credentials errors without storing token data.
- [x] 4.5 Show backend `400` field validation errors next to relevant login fields where possible.
- [x] 4.6 Redirect authenticated users away from login/register flows when appropriate.

## 5. Protected Profile and Header

- [x] 5.1 Protect `/profile` so unauthenticated users are redirected to login while auth state is resolved safely.
- [x] 5.2 Replace the profile placeholder with current user details from `ProfileResponseDto`.
- [x] 5.3 Update Header to show Login/Register links for guests.
- [x] 5.4 Update Header to show Profile/Logout controls for authenticated users.
- [x] 5.5 Ensure Header consumes shared auth state but does not own token storage or auth API calls.
- [x] 5.6 Implement logout so it clears token/profile state and returns the UI to guest navigation.

## 6. Error Handling and Preservation Checks

- [x] 6.1 Add a reusable ProblemDetail parser for form-level and field-level backend errors.
- [x] 6.2 Verify registration validation, registration conflict, login validation, invalid credentials, and expired/invalid token states manually.
- [x] 6.3 Verify public catalog search/filter behavior is unchanged for guests.
- [x] 6.4 Verify listing detail navigation, booking page loading states, and booking calendar preview/booking modes still behave as before.
- [x] 6.5 Run `npm run lint` and resolve auth-related lint errors.
- [x] 6.6 Run `npm run build` in an environment that can write TypeScript build info.
