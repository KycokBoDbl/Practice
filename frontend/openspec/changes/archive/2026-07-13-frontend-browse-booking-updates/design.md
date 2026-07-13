## Context

The current frontend already has the core browse and booking flows in place, but the next stage needs focused updates in a few user-facing areas:

- the main browsing experience should show the project banner only to guests,
- catalog search should make active filters visible and keep the filter UI discoverable while search criteria are in use,
- the listings page should refresh on its own,
- booking creation should handle duplicate requests as a recoverable conflict,
- and the listing detail calendar should stay an availability viewer instead of duplicating the booking page.

The change is frontend-only. The backend contract stays as it is, including the current `409` conflict handling for duplicate booking applications.

## Goals / Non-Goals

**Goals:**

- Preserve the current route structure, user flows, and backend API usage.
- Make the browse and booking UI more understandable without redesigning it.
- Keep the implementation incremental so lint/build can be checked after each stage.
- Build only on backend behavior that already exists.

**Non-Goals:**

- No backend API changes.
- No new booking rules or conflict rules.
- No redesign of the interface.
- No new user-facing capabilities beyond the behavior described in the spec.
- No changes to the duplicate-booking backend contract; frontend behavior must adapt to the existing `409` response.
- No repeated implementation of the already completed frontend refactoring/decomposition specs.
- No repeated mojibake cleanup or verification work from earlier specs unless touched files still require local copy fixes.

## Decisions

### 1. Keep this change focused on browse and booking updates

This stage should cover the active product problems only: guest-only banner visibility, catalog filter feedback, listings polling, duplicate booking conflict handling, and listing-detail calendar simplification.

Alternative considered: keeping the broader stabilization/decomposition scope. That would duplicate earlier refactoring specs and make the current change harder to review.

### 2. Use existing auth state for the guest-only banner

The banner should be rendered from the current auth state already available in the app. That keeps the rule local to the UI and avoids introducing a new auth source of truth.

Alternative considered: moving banner visibility into route-level configuration. That would be unnecessary for a simple guest/authenticated split.

### 3. Derive active catalog filters from the committed URL query state

The catalog already has a URL-driven search model. The cleanest implementation is to derive the visible filter summary from the committed search state rather than inventing a parallel state tree.

Alternative considered: storing a separate search-session object. That would duplicate the current URL state and complicate refresh/back-navigation behavior.

### 4. Implement listings polling as a dedicated page-owned hook

The polling logic should live in a reusable hook or page-local hook, not inline in the route shell. That keeps cleanup explicit when the page unmounts.

Alternative considered: putting polling directly in the page component. That would work, but it would make lifecycle behavior harder to isolate.

### 5. Handle duplicate booking applications with frontend conflict parsing, not backend expansion

The backend already enforces duplicate prevention and returns a `409` conflict. The frontend should map that response to a dedicated user-facing message and keep the form state intact.

Alternative considered: waiting for a richer backend error code. That would delay the frontend work and is not required for the current stage.

### 6. Keep the listing detail calendar as an availability view

The detail-page calendar should stay focused on reading availability. Booking-time selection belongs to the booking page, so the detail calendar should expose only the read-oriented view and share pure calendar helpers where useful.

Alternative considered: reusing the full booking calendar everywhere. That would keep code reuse high but would continue duplicating interaction complexity on the listing detail page.

## Risks / Trade-offs

- [Risk] Text-based duplicate-application detection can drift if backend error wording changes. Mitigation: keep the frontend mapping narrow and fail gracefully to the generic conflict message.
- [Risk] Automatic polling can increase request volume. Mitigation: keep the polling scope limited to the listings page and stop the cycle on unmount.
- [Risk] Simplifying the listing calendar may remove some interaction affordances the booking page currently provides. Mitigation: keep the booking page as the full interaction surface and limit the listing detail page to availability inspection only.
- [Risk] Catalog filter visibility can become noisy if every typed draft value is treated as active. Mitigation: distinguish draft filter state from committed URL/search state.

## Migration Plan

1. Implement the guest-only banner and catalog filter feedback first because they are local UI boundary changes.
2. Introduce listings polling with cleanup on unmount.
3. Add duplicate booking conflict handling using the existing backend response.
4. Simplify the listing calendar view while preserving the existing booking page workflow.
5. Run lint and build after each step, and finish with a focused manual check of the affected routes.

Rollback is straightforward for each step because the work is frontend-only and can be reverted feature by feature without schema or API migrations.

## Open Questions

- Should the duplicate-booking conflict message stay fully generic, or should the frontend distinguish it from other `409` booking conflicts when the backend detail text matches the current duplicate message?
- What polling interval is appropriate for the listings page so refreshes feel current without adding unnecessary load?
