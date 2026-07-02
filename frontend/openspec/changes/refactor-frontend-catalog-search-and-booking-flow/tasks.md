## 1. Catalog Search Boundary

- [ ] 1.1 Identify current search/filter URL params and document current behavior before moving logic.
- [ ] 1.2 Extract catalog query parsing and listing filtering into a catalog-owned utility or hook.
- [ ] 1.3 Move draft search/filter state and submit/reset behavior out of `Header` into a catalog-owned component or hook.
- [ ] 1.4 Update `SpacesPage` to use the extracted catalog search/filter boundary while preserving current URL params and filter results.
- [ ] 1.5 Simplify `Header` so it renders only brand/navigation and no longer imports listing API/types or owns catalog filter state.

## 2. Listing Detail Loading

- [ ] 2.1 Add a centralized `getListing(id)` API function or shared `useListing(id)` fallback based on available backend support.
- [ ] 2.2 Migrate `SpacePage` to use centralized single-listing loading.
- [ ] 2.3 Migrate `BookingPage` to use centralized single-listing loading.
- [ ] 2.4 Remove duplicated `getListings().find(...)` logic from route pages.

## 3. BookingPage States

- [ ] 3.1 Add explicit loading state handling to `BookingPage`.
- [ ] 3.2 Add explicit not-found state after loading completes with no matching listing.
- [ ] 3.3 Add explicit error state for listing load failures.
- [ ] 3.4 Verify successful listing load still renders the existing booking page content.

## 4. BookingCalendar Decomposition

- [ ] 4.1 Extract booking constants such as working hours and allowed durations from the main calendar rendering component.
- [ ] 4.2 Extract pure date/time and slot status helpers from `BookingCalendar`.
- [ ] 4.3 Extract availability loading into a dedicated hook or data boundary.
- [ ] 4.4 Refactor selected slot and duration handling to remove lint-confirmed synchronous effect-state corrections where practical.
- [ ] 4.5 Split presentational calendar sections into smaller components without changing visible behavior.
- [ ] 4.6 Verify preview mode and booking mode still preserve existing behavior.

## 5. Domain Typing

- [ ] 5.1 Convert `SPACE_TYPE_LABELS` to a typed constant map.
- [ ] 5.2 Introduce a known `SpaceType` union or equivalent derived type.
- [ ] 5.3 Update `Listing.spaceType` and label lookups to use stronger typing while keeping a safe fallback for unknown backend values.
- [ ] 5.4 Verify catalog cards and detail pages still display space type labels correctly.

## 6. Cleanup and Verification

- [ ] 6.1 Remove accidental duplicate semantic wrappers such as nested `calendarSection` containers if still present.
- [ ] 6.2 Fix heading hierarchy issues introduced or preserved during extraction.
- [ ] 6.3 Run `npm run lint` and resolve remaining lint errors related to the refactor.
- [ ] 6.4 Run `npm run build` in an environment that can write TypeScript build info.
- [ ] 6.5 Manually smoke-check catalog filtering, listing detail navigation, booking page loading states, and booking calendar preview/booking modes.
