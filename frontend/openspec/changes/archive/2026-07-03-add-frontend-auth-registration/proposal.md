## Why

The frontend currently has placeholder login and profile pages while the backend already exposes registration, login, JWT authentication, and current-profile endpoints. This change adds the frontend authentication flow needed for users to register, sign in, persist their session, and access their profile without changing the backend API.

## What Changes

- Add a registration page backed by the existing `POST /api/auth/register` endpoint.
- Add a login page backed by the existing `POST /api/auth/login` endpoint.
- Add a frontend auth API module for register, login, and `GET /api/auth/me`.
- Store the backend-issued JWT access token on the frontend without introducing refresh tokens.
- Load the current user profile from `/api/auth/me` when a token is present.
- Protect the Profile page so unauthenticated users are directed to login.
- Update Header auth navigation to show Login/Register for guests and Profile/Logout for authenticated users.
- Handle backend validation errors, invalid credentials, expired/invalid tokens, and registration conflicts.
- Preserve existing catalog, listing detail, booking page, and booking calendar behavior.

## Capabilities

### New Capabilities
- `frontend-authentication`: Frontend registration, login, JWT session storage, current-user loading, protected profile access, auth navigation, and auth error handling.

### Modified Capabilities

## Impact

- Affected frontend areas: `src/api`, auth/session state, router guards, `LoginPage`, new registration page, `ProfilePage`, and `Header`.
- Backend API usage: existing `/api/auth/register`, `/api/auth/login`, and `/api/auth/me` only.
- No backend API changes.
- No refresh token support.
- No intended behavior changes for catalog search/filtering, listing detail loading, booking routes, or booking calendar modes.
