## Why

Landlords can publish listings, and the backend now exposes owner-only listing management operations, but the frontend does not yet provide a complete workflow for managing published spaces. Adding a dedicated "My listings" experience makes landlord ownership usable without changing backend behavior.

## What Changes

- Add a landlord-only "My listings" navigation entry in the Header.
- Add a protected landlord listing management page with tabs for active, hidden, and all owned listings.
- Support editing owned listings through the existing update endpoint.
- Support hiding published listings and publishing hidden listings again.
- Support permanent deletion for listings that the backend allows to delete, with explicit confirmation and safe `409` handling when booking history blocks deletion.
- Improve booking inbox status discovery with polling and dedicated confirmed, active, completed, action-required, and all views.
- Rework the listing publication screen toward an editable listing preview experience while preserving the existing publication API contract.
- Prepare a UI placeholder for future owner organization display without depending on a backend owner-name field.
- Keep timezone behavior unchanged and do not add a deleted-listings trash flow.

## Capabilities

### New Capabilities

- `frontend-landlord-listing-management`: Landlord-owned listing list, tabs, edit, hide, activate, delete, and related states.

### Modified Capabilities

- `frontend-application-contract`: Header navigation and protected landlord management route behavior.
- `frontend-listing-publication`: Publication page presentation changes to an editable preview model.
- `frontend-booking-workflow`: Booking inbox polling and expanded status tabs for participant booking discovery.
- `backend-api-contract`: Frontend consumption of the already existing listing management endpoints without requesting backend changes.

## Impact

- Frontend routes and Header navigation.
- `src/api` listing client functions and listing domain types.
- Landlord listing management page, publication page, booking inbox page, and related CSS modules.
- Existing backend endpoints are consumed as-is; no backend implementation change is planned.
