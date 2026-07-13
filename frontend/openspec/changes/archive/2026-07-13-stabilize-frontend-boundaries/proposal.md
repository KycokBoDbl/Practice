## Why

Recent frontend review found stability and maintainability risks in protected-route redirects, user-visible mojibake text, asynchronous availability loading, booking DTO nullability, inbox polling, and oversized route components. This change stabilizes those areas before the next feature stage while preserving existing routes, backend API contracts, and visible product behavior.

## What Changes

- Standardize protected-route redirect state so `RequireAuth`, login, and registration use a single `returnTo` contract.
- Correct user-visible mojibake strings in affected frontend files without changing labels, routes, or workflows.
- Add a lightweight automated check for characteristic mojibake patterns if it can be done with the current Node/npm toolchain and without new infrastructure.
- Guard `useListingAvailability` against stale responses when listing or visible month changes quickly.
- Keep availability load failures as explicit error state instead of representing them as empty `busyIntervals`.
- Align frontend booking DTO types with backend nullable organization-name semantics.
- Move booking inbox polling/reload orchestration into a focused hook without suppressing `react-hooks/exhaustive-deps`.
- Decompose `BookingInboxPage`, `MyListingsPage`, and `ListingPublicationPage` into more stable hooks, pure helpers, and smaller presentational components where it reduces real responsibility mixing.
- Preserve existing user behavior, API paths, route paths, business rules, and visual design.

Out of scope:
- Adding `GET /api/listings/{listingId}`.
- Changing the current `getListing()` implementation or `src/api/listings.ts`.
- Any backend API or backend implementation changes.
- Solving duplicate booking request submission.
- Changing booking business rules.
- Redesigning the UI.
- Adding new user-facing capabilities.

## Capabilities

### New Capabilities
- `frontend-maintainability-guardrails`: Internal frontend quality requirements for mojibake checks and behavior-preserving decomposition boundaries.

### Modified Capabilities
- `frontend-authentication`: Protected-route return flow uses one `returnTo` state contract across route guards, login, and registration.
- `frontend-application-contract`: User-visible text remains readable and protected routes preserve existing application behavior while stabilizing internal boundaries.
- `frontend-booking-calendar`: Availability loading exposes loading/error/data separately and ignores stale responses.
- `frontend-booking-inbox`: Inbox polling is owned by a focused hook and preserves inbox refresh/filter behavior.
- `frontend-domain-typing`: Booking DTO frontend types match backend nullability for booking inbox organization names.
- `frontend-landlord-listing-management`: Listing management page is decomposed without changing landlord listing management behavior.
- `frontend-listing-publication`: Listing publication page is decomposed without changing publication behavior.

## Impact

- Affected frontend code: `src/auth`, `src/pages/Login`, `src/pages/Register`, affected page/component text literals, `src/components/BookingCalendar`, `src/pages/BookingInbox`, `src/pages/MyListings`, `src/pages/ListingPublication`, `src/types/booking`, and lightweight verification scripts if added.
- Affected OpenSpec only in this proposal phase: frontend specs and tasks under this change.
- No backend API changes.
- No route changes.
- No new runtime dependencies expected.
