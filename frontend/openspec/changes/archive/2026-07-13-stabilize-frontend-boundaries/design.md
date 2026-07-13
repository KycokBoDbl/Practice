## Context

The frontend currently has the expected high-level layers (`src/api`, `src/auth`, `src/components`, `src/pages`, `src/router`, `src/types`), but recent review identified several stability issues inside those layers:

- protected-route redirects use inconsistent router state (`from` vs `returnTo`);
- some user-visible strings contain mojibake patterns;
- `useListingAvailability` can mix stale async responses into current calendar state and collapses load failures into an empty busy interval list;
- booking inbox polling is embedded in `BookingInboxPage` and suppresses hook dependency linting;
- booking inbox DTO types assume non-null organization names even though backend DTO mapping can return `null`;
- `BookingInboxPage`, `MyListingsPage`, and `ListingPublicationPage` combine route orchestration, validation, filtering, formatting, mutation handling, and large rendering blocks.

The backend already supports the booking endpoints used by these flows. This change is intentionally frontend-only and behavior-preserving. A separate future change will address duplicate booking requests together with backend support. Another separate change would be required for `GET /api/listings/{listingId}`.

## Goals / Non-Goals

**Goals:**

- Make auth redirects consistent with a single sanitized `returnTo` contract.
- Correct existing mojibake strings without redesigning copy or UI.
- Add a lightweight mojibake verification step if feasible with the existing Node/npm setup.
- Make availability loading robust against stale responses and expose availability errors separately.
- Align booking inbox frontend DTO nullability with backend behavior.
- Extract booking inbox polling into a hook that passes hook lint rules.
- Decompose large frontend pages where extraction reduces real responsibility mixing.
- Preserve existing routes, API contracts, visual design, labels, and user workflows.

**Non-Goals:**

- Add `GET /api/listings/{listingId}`.
- Change `getListing()` or `src/api/listings.ts`.
- Change backend API or backend implementation.
- Solve duplicate booking request submission.
- Change booking business rules.
- Redesign the interface.
- Add new user-facing features.

## Decisions

1. Use `returnTo` as the single auth redirect state field.

   `RequireAuth` should write the same shape that `LoginPage`, `RegisterPage`, and `RedirectAuthenticated` already understand. The target must continue to pass through the existing safety checks. Alternative considered: teach login to also read `from`; rejected because it preserves two contracts and keeps redirect behavior easy to break again.

2. Treat mojibake cleanup as correctness, not copy redesign.

   The implementation should replace corrupted visible strings with their intended readable text, preserving meaning and current UI structure. The check should search source files for characteristic sequences such as `Р`, `С`, `в†`, `в‚`, and `рџ` only in frontend source where user-visible literals live. Alternative considered: broad encoding conversion; rejected because some files already contain valid Cyrillic and blind conversion risks corrupting good files.

3. Keep availability data state as `{ busyIntervals, loading, error }`.

   Empty intervals and failed loading must be distinguishable. Stale protection can use a cancellation flag, request id, or abortable request pattern, as long as old responses cannot overwrite the current listing/month state. Alternative considered: leave errors in `console.error`; rejected because UI and tests cannot distinguish failure from true availability.

4. Extract booking inbox polling before presentational decomposition.

   A `useBookingInbox`-style hook should own initial load, manual refresh, polling, in-flight request prevention, cleanup, and error state. The page can then focus on filters and rendering. Alternative considered: minor edits inside the page; rejected because the current effect suppresses hook dependency lint and duplicates polling logic inline.

5. Decompose large pages incrementally with route-level ownership preserved.

   `MyListingsPage` and `ListingPublicationPage` should keep route orchestration at the page boundary while extracting pure helpers, form hooks, and presentational sections. Extracted components should receive data/callback props and must not call backend APIs directly. Alternative considered: full page rewrite; rejected because this is a stabilization change and should minimize behavior risk.

6. Keep listing detail API limitations explicit.

   Existing listing detail and booking route loading can be left as-is for this change. Any endpoint addition or `getListing()` redesign requires a separate OpenSpec change involving backend contract work.

## Risks / Trade-offs

- [Risk] Mojibake pattern checks may flag legitimate text that happens to contain matching characters. -> Mitigation: keep the pattern list narrow, report file/line locations, and allow implementation to tune the check before adding it to a required script.
- [Risk] Refactoring large pages can introduce behavior drift. -> Mitigation: extract pure helpers first, keep public component props small, and run lint/build after each task group.
- [Risk] Availability error UI could look like a new user feature. -> Mitigation: use existing state-message styling and only expose the distinction needed to avoid false empty availability.
- [Risk] DTO nullability changes can reveal missing fallbacks in existing UI. -> Mitigation: update types first, then let TypeScript guide fallback handling.
- [Risk] Polling hook changes can accidentally change refresh timing. -> Mitigation: preserve the current 30-second interval and manual refresh behavior unless tests or code prove the current behavior is broken.
