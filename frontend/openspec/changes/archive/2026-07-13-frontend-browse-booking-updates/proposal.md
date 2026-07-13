## Why

The current stage focuses on browse and booking behavior that now has backend support or clear frontend-only rules. This change updates the catalog, listing detail, and booking flows without changing backend APIs or redoing the refactoring work already completed in earlier specs.

## What Changes

- Show the project information banner only to unauthenticated users.
- Keep the catalog filter popover visible while the user is actively filtering, and surface the active filter state in the search UI.
- Refresh the listings page automatically with polling, removing the need for a manual refresh button in that flow.
- Handle duplicate booking applications as a recoverable frontend conflict state using the backend's current `409` response.
- Simplify the listing calendar UI so it still shows availability clearly but no longer duplicates the richer booking-page interaction model.

## Capabilities

### New Capabilities
- `frontend-browse-booking-updates`: Covers the current stage of frontend browse and booking updates, including auth-aware banner visibility, catalog filter feedback, listings polling, duplicate booking conflict handling, and simplified listing availability display.

### Modified Capabilities
None.

## Impact

Affected areas include `src/pages/Spaces`, listing detail and booking pages, `src/auth` or shared auth state consumers, `src/api`, shared frontend booking/listing types if needed, and existing frontend verification commands.
