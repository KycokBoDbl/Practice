## Why

Two frontend reviews identified the same architectural risks: catalog search is owned by `Header`, listing detail pages duplicate broad catalog loading, and booking UI mixes data loading, state correction, business rules, and rendering. This change proposes an incremental refactor that preserves current user-visible behavior while making the frontend easier to maintain and safer to extend.

## What Changes

- Keep `Header` as a navigation-only component and move catalog search/filter business logic out of it.
- Introduce a dedicated catalog search/filter boundary that owns query-param parsing, draft filter state, city options, and filter submission behavior.
- Prepare single-listing loading via `getListing(id)` when an API endpoint exists, or a shared `useListing(id)` fallback that centralizes current `getListings().find(...)` behavior.
- Fix `BookingPage` state handling so loading, error, not found, and loaded states are distinct.
- Decompose `BookingCalendar` into smaller pieces for availability loading, date/time calculations, selection state, and presentational rendering.
- Strengthen `spaceType` typing so known space types are represented by a TypeScript union or typed constant map rather than broad `string`.
- Preserve incremental delivery: avoid large rewrites, keep routes and visible behavior stable, and validate after each step.

## Capabilities

### New Capabilities

- `frontend-catalog-search`: Covers catalog search/filter ownership, URL query-param behavior, filter derivation, and keeping `Header` navigation-only.
- `frontend-listing-detail-loading`: Covers loading a single listing for detail and booking routes, including loading/error/not found states.
- `frontend-booking-calendar`: Covers booking calendar availability loading, state model, decomposition boundaries, booking constants, and preservation of existing booking UI behavior.
- `frontend-domain-typing`: Covers typed domain constants and stronger `spaceType` typing.

### Modified Capabilities

- None.

## Impact

- Affected frontend areas: `src/components/Header/Header.tsx`, `src/pages/Spaces/SpacesPage.tsx`, `src/pages/Spaces/SpacePage.tsx`, `src/pages/Booking/BookingPage.tsx`, `src/components/BookingCalendar/BookingCalendar.tsx`, `src/api/listings.ts`, `src/types/listing.ts`, and `src/types/spaceType.ts`.
- API impact: may use a future `GET /api/listings/:id`-style endpoint through `getListing(id)`; if unavailable, implementation should centralize the existing list-and-find fallback without changing backend behavior.
- Behavior impact: no intentional user-visible behavior changes; refactor must preserve current routes, search params, filter semantics, listing display, booking calendar display, and navigation.
- Verification impact: lint should pass after hook/state refactors; build should be checked in an environment that can write TypeScript build info.
