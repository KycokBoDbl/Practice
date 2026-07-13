## Why

Users can see a listing address as text, but they cannot quickly inspect where the space is located. The backend already geocodes listing addresses and returns coordinates, so the frontend can add a map view on the listing detail page without introducing new backend API work.

## What Changes

- Add a user-visible `Посмотреть на карте` action on the listing detail page when map coordinates are available.
- Open a Yandex Maps view focused on the selected listing coordinates and show the listing title/address as context.
- Handle listings without coordinates with a clear non-blocking fallback instead of attempting frontend geocoding.
- Keep existing catalog, listing detail, booking, publication, and listing management behavior unchanged.
- Do not add backend endpoints, change listing API contracts, or geocode addresses in the frontend.

## Capabilities

### New Capabilities
- `frontend-listing-map-view`: Frontend map viewing behavior for the listing detail page using backend-provided coordinates.

### Modified Capabilities

## Impact

- Affected frontend areas: listing detail page, shared listing UI components, and any map-specific component/hook introduced for this change.
- Affected types: existing `Listing.latitude` and `Listing.longitude` are used as nullable coordinate inputs.
- Potential dependency impact: a frontend Yandex Maps integration may be introduced if the current project has no existing map renderer.
- Backend impact: none. The implementation must use existing `GET /api/listings` data and must not require a new backend API.
