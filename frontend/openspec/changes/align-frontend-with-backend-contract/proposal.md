## Why

The backend now exposes a complete authenticated booking workflow, while the frontend still stops at slot selection and price preview. This change turns the current frontend into a contract-aligned client for the existing backend API so tenants can create bookings and participants can inspect booking state.

## What Changes

- Add a frontend booking API boundary for create, read, transition, and history endpoints using the existing backend OpenAPI shapes.
- Wire the booking calendar confirmation action to `POST /api/bookings` for authenticated tenants.
- Route guests who attempt booking confirmation into the existing login/registration flow without losing their selected listing and slot context.
- Show booking request outcomes, recoverable conflicts, unauthorized states, and refreshed availability after successful or conflicting booking attempts.
- Add participant-facing booking detail and history loading for existing bookings.
- Keep public catalog, listing detail, availability preview, auth registration/login/profile, and existing calendar behavior intact.
- No backend API changes are planned.

## Capabilities

### New Capabilities
- `frontend-booking-workflow`: Frontend booking API client, booking submission, participant booking detail, booking transitions, and booking history UI.

### Modified Capabilities
- `frontend-application-contract`: Booking submission and participant booking-state requirements become implementation-ready frontend behavior.
- `frontend-booking-calendar`: The booking calendar confirm action changes from a disabled visual endpoint into a backend-backed booking request flow.
- `frontend-authentication`: Auth routing must preserve intended booking context when guests are redirected to login or registration.

## Impact

- Affected frontend areas: `src/api`, `src/types`, `src/components/BookingCalendar`, `src/pages/Booking`, `src/router`, auth route guards, and new or updated booking status UI.
- Backend API usage: existing `/api/bookings`, `/api/bookings/{bookingId}`, `/api/bookings/{bookingId}/history`, `/api/bookings/{bookingId}/approve`, `/api/bookings/{bookingId}/reject`, `/api/bookings/{bookingId}/confirm`, and `/api/bookings/{bookingId}/cancel`.
- Error handling: reuse the existing ProblemDetail parser and refresh listing availability after `409 Conflict` booking responses.
- Verification: run frontend lint/build and manually check guest, tenant, landlord, conflict, and unauthorized booking paths against the existing backend contract.
