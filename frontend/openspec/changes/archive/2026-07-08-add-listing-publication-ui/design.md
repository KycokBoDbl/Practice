## Context

The backend exposes `POST /api/listings` as a bearer-authenticated landlord operation. The request shape is `CreateListingRequestDto`: `title`, optional `description`, `city`, `address`, `pricePerHour`, `capacity`, `spaceType`, and optional `imageUrl`. The successful response is `ListingResponseDto`, and `description`/`imageUrl` may be `null`.

The frontend already has a shared Axios API client with bearer token handling, auth state with `profile.role`, protected route wrappers, catalog listing cards, listing detail pages, and typed space type labels. Publication should use those existing boundaries instead of adding new auth or transport patterns.

## Goals / Non-Goals

**Goals:**
- Let authenticated landlords publish listings from the frontend.
- Keep guests and tenants out of the publication workflow with clear routing and messaging.
- Make the form practical for repeated use: city autocomplete, compact amenity chips, price formatting, image placeholder, and a final summary.
- Submit only the backend-supported request shape and avoid backend-dependent features that do not exist yet.
- Preserve current catalog, listing detail, booking, booking inbox, and auth behavior.

**Non-Goals:**
- Backend changes.
- File upload or image hosting.
- Draft synchronization across devices.
- Editing, unpublishing, or managing a landlord's existing listings.
- AI-generated descriptions.
- A separate live catalog-card preview panel.
- Autogenerating listing titles from city/type.

## Decisions

### Decision: Publish through a dedicated landlord-only route

Use a dedicated route such as `/spaces/new` for the publication page. The route should be auth-protected and role-aware: guests go through existing auth flow, tenants see a safe forbidden state, and landlords can submit the form.

Alternative considered: put publication inside the profile page. That would hide a primary landlord workflow inside account details and make navigation less direct.

### Decision: Keep publication data in the listing API boundary

Add `publishListing` to the existing listing API module and represent `CreateListingRequest` in the listing type boundary. The shared API client already attaches bearer auth, so the publication client should not duplicate auth logic.

Alternative considered: create a separate landlord API module. That would be premature while the only landlord listing operation is publication.

### Decision: City autocomplete is local and Russia-only

Use a broad static list of Russian cities in the frontend, combine it with cities already returned by `GET /api/listings`, and let users type a custom value if their city is missing. This avoids network dependencies while satisfying the need for fast suggestions.

Alternative considered: third-party geocoding/autocomplete. That adds external dependency, network reliability concerns, API keys, and privacy considerations for a feature that can start locally.

### Decision: Amenity chips are encoded into description text

Because the backend publication contract has no separate tags field, selected amenity chips should be appended or inserted into the submitted description in a readable text form. Listing detail and catalog can then display them as part of description without backend schema changes.

Alternative considered: store tags in local frontend state. That would disappear across devices and not be visible to other users.

### Decision: Use image placeholder for missing images

The backend allows `imageUrl: null`, so catalog/detail/publication success UI must not assume a real image URL. The frontend should use a reusable placeholder asset/style when no URL is present or when the user leaves image URL empty.

Alternative considered: require image URL in the frontend. That would be stricter than backend behavior and make publishing harder.

### Decision: No full card preview in this change

The form will include lightweight visual feedback: image placeholder, price formatting, and final summary. A full live card preview is intentionally excluded to keep the page focused and implementation smaller.

Alternative considered: split form and live preview into two columns. This adds layout and maintenance weight without being essential to publication correctness.

## Risks / Trade-offs

- [Risk] Static city list can be incomplete. -> Allow free text and combine with catalog-derived cities.
- [Risk] Amenity chips stored in description are less structured than real tags. -> Keep the inserted format readable and reversible enough for future migration.
- [Risk] Large chip base can crowd the form. -> Use compact grouped chips with search/filter or collapsed sections.
- [Risk] Image URLs can be broken. -> Validate URL shape before submit and render a placeholder if the value is empty; broken remote images can still fall back visually.
- [Risk] Tenant/guest access edge cases. -> Reuse existing auth guard and role checks rather than relying only on hidden navigation.
