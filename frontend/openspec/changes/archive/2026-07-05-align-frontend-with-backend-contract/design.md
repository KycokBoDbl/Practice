## Context

The frontend already has a shared axios client, token storage, auth provider, protected profile route, public listing catalog, listing detail page, and booking calendar that loads backend availability. The backend already exposes the authenticated booking workflow and generated OpenAPI contract, including `POST /api/bookings`, participant booking lookup, history, and transition commands.

The main gap is behavioral: the frontend can select an available slot but does not create a backend booking, does not preserve booking intent through login, and does not display the resulting participant booking state.

## Goals / Non-Goals

**Goals:**
- Add typed frontend booking API functions that match the existing backend contract.
- Submit booking requests from the booking calendar/page using selected date, start time, and duration.
- Preserve guest booking intent through login or registration before retrying a protected booking operation.
- Show booking success, validation/conflict/unauthorized errors, and refresh availability when backend state may have changed.
- Add participant-facing booking status and history UI using existing backend endpoints.
- Keep public catalog, listing detail, auth, and current calendar selection behavior stable.

**Non-Goals:**
- No backend endpoint, DTO, database, security, or OpenAPI generation changes.
- No refresh token, email verification, password recovery, or organization user-management work.
- No landlord listing-owner management UI unless required to consume an existing booking endpoint.
- No broad visual redesign of catalog, header, auth pages, or listing detail.

## Decisions

1. Add a dedicated `src/api/bookings.ts` module and `src/types/booking.ts`.

Rationale: booking workflow has multiple endpoints and domain states. Keeping it separate from `listings.ts` avoids mixing public discovery API with authenticated workflow commands. The types should mirror the backend OpenAPI names and enum values so drift is visible.

Alternatives considered: adding booking calls beside listing availability in `listings.ts`. That is simpler initially but makes public listing reads and protected booking commands harder to reason about.

2. Let the booking page own submission orchestration and keep the calendar mostly selection-focused.

Rationale: `BookingCalendar` already calculates available slots and duration. The page has route/listing context and can coordinate auth redirects, API calls, and navigation. The calendar should expose a confirm callback or selected booking payload rather than owning router/auth/API concerns.

Alternatives considered: calling `POST /api/bookings` directly inside `BookingCalendar`. That would couple a reusable UI component to auth and routing.

3. Preserve booking intent in router state for guest flows.

Rationale: the user can select a slot before authentication. Router state can carry `returnTo` and the selected booking payload through login/register without introducing global draft storage. If state is lost on refresh, the user can reselect the slot from the booking page.

Alternatives considered: localStorage draft persistence. It is more durable but introduces expiry, stale listing, and cross-account edge cases that are unnecessary for this scope.

4. Treat `409 Conflict` as recoverable and refresh availability.

Rationale: conflicts can happen when another booking holds the slot after the frontend loaded availability. Refreshing the visible month lets the UI converge with backend truth and gives the user a clear next action.

Alternatives considered: only showing the error. That leaves the calendar visually stale.

5. Add a participant booking detail route before adding list dashboards.

Rationale: backend exposes lookup by booking id and history, but no list-by-current-user endpoint. A detail route lets the frontend consume available participant state without requiring backend API expansion.

Alternatives considered: building tenant/landlord booking dashboards. That would require new backend list endpoints and is outside this frontend-only change.

## Risks / Trade-offs

- Booking intent in router state can be lost on full page reload -> The booking page remains the source of truth and users can reselect the slot.
- Frontend cannot discover all user bookings without a list endpoint -> Limit this change to booking creation success and direct participant detail/history by id.
- Tenant-only creation is enforced by backend, not solely frontend -> Check profile role to improve UX, but still handle `403` from the backend.
- Existing Russian text appears mojibake in source output -> Keep edits ASCII-compatible where possible and avoid unnecessary copy changes outside touched UI.
- Backend uses local date-time strings without offsets -> Continue using the existing `YYYY-MM-DDTHH:00` helpers and do not introduce timezone conversion in this change.

## Migration Plan

1. Add booking types and API functions.
2. Refactor booking calendar/page to emit and submit selected booking payloads.
3. Add auth redirect state for guest booking attempts.
4. Add booking result/detail/history UI.
5. Verify lint/build and manual flows against the existing backend.

Rollback is straightforward: remove the new booking API/UI wiring and restore the booking calendar confirm button to non-submitting behavior. No backend or data migration is involved.
