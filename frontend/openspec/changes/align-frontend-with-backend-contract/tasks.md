## 1. Booking Contract Types and API

- [ ] 1.1 Add booking domain types for statuses, create request, booking response, history response, and transition command responses in `src/types`.
- [ ] 1.2 Add `src/api/bookings.ts` with functions for create booking, get booking, get booking history, approve, reject, confirm, and cancel.
- [ ] 1.3 Reuse the shared axios client so booking API calls automatically send bearer tokens and clear auth state on `401`.
- [ ] 1.4 Add a small booking error classification helper or reuse `parseApiError` so `400`, `401`, `403`, `404`, and `409` can be rendered consistently.

## 2. Calendar Selection Payload

- [ ] 2.1 Refactor `BookingCalendar` to expose selected booking payload data through a callback or confirm handler without owning backend API calls.
- [ ] 2.2 Calculate `startAt` and exclusive `endAt` from selected date, start hour, and resolved duration using existing whole-hour local-time helpers.
- [ ] 2.3 Add parent-controlled submitting/disabled state to the booking confirm control to prevent duplicate submission.
- [ ] 2.4 Preserve preview mode, busy interval rendering, duration resolution, and existing unavailable-slot behavior.

## 3. Booking Creation Flow

- [ ] 3.1 Update `BookingPage` to submit selected booking payloads through the booking API for authenticated tenants.
- [ ] 3.2 Show successful booking creation state with booking id, status, interval, total price, and a link to booking detail.
- [ ] 3.3 Prevent known landlords from submitting tenant booking requests and show an account-role message.
- [ ] 3.4 Handle `400`, `403`, and `404` booking creation responses without clearing listing context.
- [ ] 3.5 Handle `409 Conflict` by showing a recoverable conflict message and refreshing listing availability for the visible calendar period.

## 4. Guest Booking Intent and Auth Return

- [ ] 4.1 When a guest confirms a booking selection, route them to login or registration with return context for the booking page.
- [ ] 4.2 Update login success handling to return to the original booking route when valid return context exists.
- [ ] 4.3 Update registration success handling to preserve the intended booking return path through the login step where possible.
- [ ] 4.4 Add fallback handling for missing or stale return context so the app returns to the booking page or catalog without throwing.

## 5. Booking Detail and History UI

- [ ] 5.1 Add a booking detail route that requires authentication.
- [ ] 5.2 Add a booking detail page that loads `GET /api/bookings/{bookingId}` and renders loading, loaded, unavailable, forbidden, and error states.
- [ ] 5.3 Render booking status, listing id, interval, price per hour, total price, and confirmation deadline when present.
- [ ] 5.4 Load and render booking history from `GET /api/bookings/{bookingId}/history` in backend order.
- [ ] 5.5 Link successful booking creation to the new booking detail route.

## 6. Participant Transition Controls

- [ ] 6.1 Show approve and reject controls for known landlord users viewing a `REQUESTED` booking.
- [ ] 6.2 Show confirm and cancel controls for known tenant users viewing an `AWAITING_CONFIRMATION` booking.
- [ ] 6.3 Hide mutating controls for terminal statuses and unsupported role/status combinations.
- [ ] 6.4 Submit transition actions through the booking API and update the visible booking state from backend responses.
- [ ] 6.5 Refresh booking history after successful transitions and show conflict/forbidden errors when transitions are rejected.

## 7. Verification

- [ ] 7.1 Run `npm run lint` and resolve frontend lint issues introduced by the change.
- [ ] 7.2 Run `npm run build` and resolve TypeScript or Vite build issues.
- [ ] 7.3 Manually verify guest booking redirect, tenant booking creation, landlord role prevention, conflict handling, booking detail loading, history rendering, and transition error states.
- [ ] 7.4 Verify existing public catalog, listing detail, auth registration/login/profile, and booking calendar preview behavior still works.
