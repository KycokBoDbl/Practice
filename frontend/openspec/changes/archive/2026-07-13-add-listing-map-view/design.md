## Context

The backend already stores geocoded listing coordinates and returns `latitude` and `longitude` in the public listing response. The frontend `Listing` type already models those fields as nullable numbers. Current listing UI shows the address as text, but there is no map action or map surface on the listing detail page.

This change is frontend-only. It must use existing listing data from `GET /api/listings` and must not introduce a new backend endpoint or frontend-side geocoding flow.

## Goals / Non-Goals

**Goals:**
- Add a clear map entry point on the listing detail page for listings with coordinates.
- Display a focused map view for the selected listing, including a marker and listing context.
- Keep map logic isolated from the listing detail page and other layout components.
- Preserve existing catalog, listing detail, booking, publication, and landlord management behavior.
- Handle missing coordinates gracefully.

**Non-Goals:**
- No backend API changes.
- No `GET /api/listings/{listingId}` dependency.
- No frontend geocoding by address text.
- No catalog-card map action in this change.
- No map-based search, filtering by map bounds, route planning, clustering, or multi-listing map mode.
- No changes to listing publication validation or address business rules.

## Decisions

1. Use backend-provided coordinates as the only source of map positioning.

   Rationale: the backend already geocodes `city + address` during listing publication/update and returns coordinates. Re-geocoding in the frontend would duplicate backend responsibility, require extra provider configuration, and risk inconsistent results.

   Alternatives considered:
   - Frontend geocoding from address text: rejected because it duplicates backend logic and requires new client-side provider concerns.
   - Adding a listing map endpoint: rejected because existing listing responses already contain the required data.

2. Introduce a small frontend map boundary instead of embedding provider code directly into the page.

   Rationale: the page should own user flow and state, while the map component owns provider rendering, marker display, loading/error/fallback UI, and cleanup.

   Expected boundary:
   - A reusable map view component receives title, address, latitude, longitude, and close/open state from the parent.
   - A small helper validates coordinates and formats the address label.
   - Provider-specific implementation details stay inside the map component or a map adapter module.

3. Open the map from the listing detail page without changing routes.

   Rationale: the requested interaction belongs to the listing detail experience, not a new navigation workflow. A modal or inline expandable panel preserves the current page context and avoids new route semantics.

   Alternatives considered:
   - Dedicated route per map: rejected for this change because it adds navigation surface and deep-link behavior that was not requested.
   - External map link only: rejected as the primary UX because the user asked for the map to open in the application flow. It can remain a secondary provider link if useful.

4. Use Yandex Maps with a frontend-specific key.

   Rationale: the backend already uses Yandex geocoding, so keeping the map provider aligned reduces geographic drift. The browser integration should use its own frontend env key and never reuse backend credentials.

## Risks / Trade-offs

- [Map provider dependency adds bundle/runtime surface] -> Keep provider code isolated and lazy-load it if practical.
- [External map tiles or SDK can fail to load] -> Provide a non-blocking error state that still shows the address and does not break listing browsing.
- [Some listings may have null coordinates] -> Disable or replace the map action with a clear unavailable state instead of opening an empty map.
- [Coordinates could be stale after address edits until backend response refreshes] -> Use the latest listing object returned by existing list/publication/update flows.
- [Provider keys may be needed later] -> Keep configuration frontend-owned and documented; do not require backend API changes.
