## 1. Preparation and boundaries

- [x] 1.1 Re-read proposal, design, and spec before implementation.
- [x] 1.2 Inspect the listing detail page, listing types, and API code to identify the narrow frontend touch points.
- [x] 1.3 Confirm implementation can use existing `Listing.latitude` and `Listing.longitude` without backend API, backend code, or database changes.
- [x] 1.4 If a backend change appears necessary, stop implementation and explain the required backend change before editing anything outside frontend.

## 2. Low-risk coordinate helpers and state boundaries

- [x] 2.1 Add a small frontend helper for validating complete listing coordinates.
- [x] 2.2 Define the selected-listing map state shape without changing existing listing API functions.
- [x] 2.3 Wire the frontend map provider path for Yandex Maps and document any frontend-only dependency or environment requirement.
- [x] 2.4 Run lint/build after helper and state-boundary changes.

## 3. Map entry point on listing detail

- [x] 3.1 Add the `Посмотреть на карте` action to the listing detail page.
- [x] 3.2 Show a clear unavailable state when a listing has missing coordinates instead of opening a map.
- [x] 3.3 Preserve existing detail navigation, booking entry, filters, and polling behavior.
- [x] 3.4 Run lint/build after adding the map entry point.

## 4. Map view implementation

- [x] 4.1 Implement an isolated map view component that receives listing title, address, latitude, longitude, and close/open handlers.
- [x] 4.2 Center the map on the selected listing coordinates and render a marker for the listing.
- [x] 4.3 Show listing title and address near the map or marker.
- [x] 4.4 Keep Yandex Maps provider code contained inside the map boundary or a dedicated adapter module.
- [x] 4.5 Add recoverable loading/error/fallback UI for map provider or tile failures.
- [x] 4.6 Ensure closing the map returns the user to the same detail context without changing route semantics.
- [x] 4.7 Run lint/build after the map view implementation.

## 5. Visual and workflow verification

- [x] 5.1 Manually check the listing detail map action for a listing with coordinates.
- [x] 5.2 Manually check a listing without coordinates or simulated missing coordinates.
- [x] 5.3 Manually check mobile and desktop layout for map modal/panel sizing, text fit, and visual correctness.
- [x] 5.4 Manually check that booking, listing publication, listing management, catalog filters, and listing polling still behave as before.

## 6. Final verification and handoff

- [x] 6.1 Run the full frontend verification available in the project.
- [x] 6.2 Run OpenSpec validation for `add-listing-map-view`.
- [x] 6.3 Review changed files and confirm the implementation stayed inside frontend scope.
- [x] 6.4 Summarize changed files, completed behavior, validation results, and any manual checks still needed.
