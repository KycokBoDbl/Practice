## 1. Auth Redirect Stabilization

- [x] 1.1 Update protected route redirect state so `RequireAuth` writes a sanitized `returnTo` value instead of a separate `from` contract.
- [x] 1.2 Verify `LoginPage`, `RegisterPage`, and `RedirectAuthenticated` preserve and consume the same `returnTo` contract.
- [x] 1.3 Manually verify guest access to `/profile`, `/bookings`, `/my-listings`, and `/spaces/new` returns to the requested route after login.

## 2. Text Encoding And Guardrail

- [x] 2.1 Identify frontend source files with user-visible mojibake strings.
- [x] 2.2 Replace corrupted user-visible strings with readable text while preserving existing labels, meanings, routes, and UI layout.
- [x] 2.3 Add a lightweight mojibake-pattern verification script or npm command if it can be implemented with the existing Node/npm toolchain and no new infrastructure.
- [x] 2.4 Run the mojibake check, if added, and confirm it reports no affected frontend source.

## 3. Availability Loading Stability

- [x] 3.1 Update `useListingAvailability` state shape to expose busy intervals, loading state, and error state separately.
- [x] 3.2 Guard availability loading so stale responses from old listing/month/refresh combinations cannot overwrite current state.
- [x] 3.3 Update `BookingCalendar` and related presentational sections to preserve current calendar behavior while handling availability errors distinctly from empty busy intervals.
- [x] 3.4 Verify rapid month changes and refreshes do not produce stale visible availability.

## 4. Booking DTO Typing

- [x] 4.1 Align `BookingInboxItem` organization-name fields with backend nullable semantics.
- [x] 4.2 Update booking inbox rendering helpers to show safe fallbacks for missing tenant or landlord organization names.
- [x] 4.3 Confirm booking inbox API usage still matches `GET /api/bookings` without backend response shape changes.

## 5. Booking Inbox Decomposition

- [x] 5.1 Extract booking inbox loading, manual refresh, polling, in-flight request prevention, cleanup, and error handling into a focused hook.
- [x] 5.2 Remove the local `react-hooks/exhaustive-deps` suppression from booking inbox polling code.
- [x] 5.3 Extract or isolate inbox status formatting, summary counts, filtering, and card/list presentation where it reduces `BookingInboxPage` responsibility.
- [x] 5.4 Preserve existing inbox filters, 30-second polling behavior, manual refresh behavior, and detail links.

## 6. Listing Management Decomposition

- [x] 6.1 Extract pure listing management helpers for description/amenity parsing, formatting, validation, and payload construction.
- [x] 6.2 Extract landlord owned-listing load and mutation orchestration into a hook or smaller route-level data boundary.
- [x] 6.3 Extract management list, status filters, listing card, and edit form sections into smaller presentational components where practical.
- [x] 6.4 Preserve existing hide, activate, delete, edit, conflict, forbidden, not-found, empty, and loaded states.

## 7. Listing Publication Decomposition

- [x] 7.1 Extract publication validation and payload construction into pure helpers.
- [x] 7.2 Extract publication form state and helper-chip selection into a focused hook or smaller page-local module.
- [x] 7.3 Extract publication form sections, preview, helper chips, and status panels into smaller presentational components where practical.
- [x] 7.4 Preserve current publication request shape, preview behavior, validation, first-error focus, pending state, and success path.

## 8. Verification

- [x] 8.1 Run `npm run lint` and fix issues introduced by this change.
- [x] 8.2 Run `npm run build` and fix TypeScript/build issues introduced by this change.
- [x] 8.3 Manually verify protected-route return flow, booking calendar availability error/loading behavior, booking inbox polling/refresh, landlord listing management, and listing publication.
- [x] 8.4 Confirm no backend files, backend APIs, `src/api/listings.ts`, or `getListing()` behavior were changed.
