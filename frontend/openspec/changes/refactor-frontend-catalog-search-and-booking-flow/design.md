## Context

The current frontend is a React + TypeScript + Vite application with a small but growing catalog and booking flow. Reviews in `openspec/reviews/frontend-review-v1.md` and `openspec/reviews/frontend-review-v2.md` identified recurring issues:

- `Header` loads listing data and owns catalog search/filter draft state.
- `SpacesPage`, `SpacePage`, and `BookingPage` duplicate listing loading behavior.
- `BookingPage` renders not-found UI before listing loading completes.
- `BookingCalendar` combines availability API calls, calendar math, selection correction, business constants, and rendering.
- `spaceType` is modeled as broad `string` despite a known label map.

The refactor must preserve current visible behavior while improving ownership boundaries and testability.

## Goals / Non-Goals

**Goals:**

- Keep `Header` focused on navigation and brand display.
- Centralize catalog query parsing, draft filter state, and listing filtering outside `Header`.
- Centralize single-listing loading for detail and booking pages.
- Make `BookingPage` loading, error, not found, and loaded states explicit.
- Decompose `BookingCalendar` incrementally without changing the booking UI.
- Strengthen `spaceType` typing while preserving API compatibility.
- Keep the implementation incremental and reviewable.

**Non-Goals:**

- Redesign the user interface.
- Change existing routes or URL query parameter names.
- Change search/filter semantics unless required to preserve existing behavior in a clearer boundary.
- Require backend changes before frontend refactoring can start.
- Introduce a global state library or data-fetching dependency.
- Implement booking submission behavior beyond the existing UI surface.

## Decisions

### Decision: Move catalog search/filter ownership out of Header

`Header` will no longer call `getListings()`, derive city options, own filter draft state, or submit catalog filters. Catalog search/filter behavior will move into a catalog-owned component or hook used by `SpacesPage`.

Rationale: Navigation is rendered on every route. Keeping business data loading in `Header` creates unnecessary requests and couples unrelated routes to catalog state.

Alternative considered: Keep the search UI in `Header` but move only data loading to context. This still leaves navigation responsible for catalog behavior and introduces shared state before it is needed.

### Decision: Preserve URL query params as the committed catalog state

Current search and filter behavior is expressed through URL params such as `q`, `city`, `minCapacity`, `maxCapacity`, `minPrice`, and `maxPrice`. The refactor will keep these params stable and treat them as committed filter state.

Rationale: This preserves deep-linking, back/forward behavior, and current route behavior while allowing draft form state to live in a smaller catalog search boundary.

Alternative considered: Move filters entirely to local component state. That would simplify code but would change shareable URLs and navigation behavior.

### Decision: Centralize single-listing loading behind an API/helper boundary

The implementation will prefer `getListing(id)` if the backend supports a single-listing endpoint. If it does not, a shared `useListing(id)` or API helper will temporarily centralize the existing `getListings().find(...)` fallback.

Rationale: `SpacePage` and `BookingPage` should not duplicate broad catalog loading. Centralizing the fallback makes the later backend endpoint switch localized.

Alternative considered: Leave each page to fetch all listings. This keeps the current behavior but preserves duplication and inconsistent loading/error states.

### Decision: Split BookingCalendar by responsibility before changing behavior

`BookingCalendar` will be decomposed into smaller units:

- availability loading hook
- pure calendar/date/time helpers
- selection-state logic
- presentational calendar/month/slot/duration sections
- booking constants or config

Rationale: The current component is large and has lint-confirmed hook/state problems. Splitting responsibility first reduces risk before behavior changes.

Alternative considered: Rewrite the booking flow in one pass. This increases regression risk and conflicts with the requested incremental approach.

### Decision: Strengthen domain typing with typed constants

`SPACE_TYPE_LABELS` will become a typed constant, and `Listing.spaceType` will use a derived union or compatible domain type where possible.

Rationale: The current `Record<string, string>` and `spaceType: string` combination hides invalid values from TypeScript. A typed map preserves runtime fallback behavior while improving compile-time safety.

Alternative considered: Use a TypeScript enum. A typed constant map is lighter, keeps labels close to keys, and avoids enum runtime shape concerns.

## Risks / Trade-offs

- Existing filter behavior changes accidentally → Preserve query param names and add focused checks for search, city, capacity, and price filters.
- Backend lacks `getListing(id)` endpoint → Use a shared fallback helper/hook first and isolate the future endpoint switch.
- BookingCalendar decomposition introduces regressions → Extract pure helpers first, then hooks, then presentational components, validating after each step.
- Stronger `spaceType` typing conflicts with unknown backend values → Keep a display fallback for unknown values while narrowing known values in frontend constants.
- Lint/build verification may be environment-sensitive → Run lint after each implementation phase and run build in an environment that can write TypeScript build info.

## Migration Plan

1. Add centralized catalog filter utilities/hook while keeping existing URL params and filter semantics.
2. Move search/filter UI ownership out of `Header`; verify `Header` remains navigation-only.
3. Add centralized single-listing loading and migrate `SpacePage` and `BookingPage`.
4. Fix `BookingPage` loading, error, not found, and loaded states.
5. Extract booking calendar helpers and availability hook.
6. Split booking calendar presentational sections while preserving rendered behavior.
7. Tighten `spaceType` typing and update usages.
8. Run lint and build verification.

Rollback is straightforward because each step is scoped: revert the latest extracted hook/component or restore the prior page-level usage while keeping earlier completed steps intact.

## Open Questions

- Does the backend already expose a single-listing endpoint, or should the first implementation use a frontend fallback helper?
- Should catalog city options come from loaded listings, a dedicated endpoint, or a static/domain source?
- Should booking business constants remain frontend-owned, or should they eventually come from listing availability metadata?
