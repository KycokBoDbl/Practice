## Why

Landlords can register and own listings, and the backend now exposes a protected publication endpoint, but the frontend has no way for a landlord to create a listing from the application. Adding a focused publication UI completes the landlord-side marketplace loop without requiring backend changes.

## What Changes

- Add typed frontend support for publishing listings through `POST /api/listings`.
- Add a protected landlord-only publication route for creating a listing.
- Add authenticated navigation to the publication flow for landlords only.
- Build a user-friendly listing publication form with client-side validation, backend error handling, and success navigation to the created listing.
- Add a Russian city autocomplete with a broad built-in Russian city base and support for custom typed values.
- Add a compact amenity/tag chip selector whose selected values are included in the submitted listing description and later visible as part of listing text.
- Support nullable `description` and `imageUrl` from the backend contract, including an image placeholder when no image URL is provided.
- Add price formatting and a publication summary before submit.
- Preserve existing catalog, listing detail, booking, booking inbox, and authentication behavior.

## Capabilities

### New Capabilities
- `frontend-listing-publication`: Landlord-only listing publication page and submission workflow.
- `frontend-listing-publication-assistants`: UI helpers for city autocomplete, amenity chips, image placeholder, price formatting, and publication summary.

### Modified Capabilities
- `backend-api-contract`: Frontend consumes the backend listing publication endpoint and nullable listing fields.
- `frontend-application-contract`: Authenticated landlord navigation and protected publication route are added.
- `frontend-domain-typing`: Listing publication request types and nullable listing response fields are represented safely.

## Impact

- Frontend API/types: extend listing API and domain types.
- Router/navigation: add a protected landlord-only publication route and Header entry.
- UI: add a publication page with form, validation, helper controls, submit states, and success flow.
- Existing catalog/detail: handle missing listing images safely through a placeholder.
- Backend: no backend implementation change is required.
