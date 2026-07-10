## Context

The current listing module supports public reads of `PUBLISHED` listings and authenticated landlord publication through `POST /api/listings`. Listings already store `owner_organization_id`, but there is no API for the owner to modify or remove listings after publication. Booking workflow references `listings.id` through `bookings.listing_id`, while listing unavailability periods reference listings with `ON DELETE CASCADE`.

The management workflow must preserve public catalog behavior, booking history, and tenant isolation. Frontend developers also need the new protected operations in the runtime and exported OpenAPI contract.

## Goals / Non-Goals

**Goals:**
- Add owner-only landlord operations for full listing edit, hide, reactivate, and delete.
- Keep tenant and non-owner organizations from mutating listings.
- Make hidden listings disappear from `GET /api/listings`, availability lookup, and new booking creation.
- Preserve booking history by preventing hard deletion when a booking references the listing.
- Publish the changed runtime OpenAPI and generated JSON contract.

**Non-Goals:**
- Public detail pages for hidden listings.
- Partial patch semantics with absent-field vs null-field tracking.
- Bulk listing management.
- Frontend implementation.

## Decisions

1. Use `PUT /api/listings/{listingId}` for editing.

   A full replacement request reuses the existing validation model for listing publication and avoids ambiguous partial-update semantics for nullable `description` and `imageUrl`. Alternative considered: `PATCH` with partial DTO. That would require extra field-presence tracking to distinguish omitted fields from explicit nulls, adding complexity not needed for the MVP.

2. Use command endpoints for visibility changes.

   `POST /api/listings/{listingId}/hide` changes lifecycle state from `PUBLISHED` to `ARCHIVED`; `POST /api/listings/{listingId}/activate` changes it back from `ARCHIVED` to `PUBLISHED`. Both return `204 No Content` and are idempotent for their target state. Alternative considered: `PATCH /api/listings/{id}` with a client-controlled status field. That would expose server-controlled lifecycle state in a general edit DTO and repeat the publication design issue where clients must not choose owner/status/createdAt.

3. Extend `ListingStatus` with `ARCHIVED`.

   Public catalog and availability already query by `PUBLISHED`, so adding `ARCHIVED` keeps hidden listings out of tenant-facing discovery and booking creation with minimal query churn. Reactivation simply restores `PUBLISHED`, making the listing visible again to public catalog, availability, and new booking creation. A Flyway migration should update any database enum/check constraint if present. Alternative considered: adding a boolean `hidden` column. Status scales better for future states and keeps one lifecycle field.

4. Implement hard delete only when no booking references the listing.

   `DELETE /api/listings/{listingId}` returns `204 No Content` when the owned listing has no bookings. If bookings exist, it returns `409 Conflict` and the landlord can hide the listing instead. This preserves booking history and avoids breaking the `bookings.listing_id` foreign key. Listing unavailability periods can be removed with the listing when deletion is allowed because their FK already cascades.

5. Use ownership-aware loading in `ListingService`.

   Mutating operations should require `LANDLORD`, then load by listing id and `ownerOrganization.id`. Missing or non-owned listings should return `404 Not Found` to avoid exposing cross-organization listing existence. Non-landlord roles return `403 Forbidden`; missing/invalid authentication remains `401 Unauthorized`.

6. Keep `ListingResponseDto` camelCase.

   New responses should continue using `ListingResponseDto` with existing camelCase field names, including `ownerOrganizationId`. New request DTOs must not accept `ownerOrganizationId`, `status`, or `createdAt`.

## Risks / Trade-offs

- Hard delete can surprise landlords when bookings exist -> return `409 Conflict` with a clear problem detail and document hiding as the safe alternative.
- Hidden listings may still have future bookings -> hide only prevents new discovery/booking; it does not cancel existing bookings.
- Reactivation can make stale listing details visible again -> use the same owner-only edit endpoint before activation when details need correction.
- Status migration can drift from Java enum -> cover with schema tests and integration tests that archive and reactivate a listing, confirming catalog exclusion and inclusion.
- Reusing listing validation in a second DTO can duplicate annotations -> keep duplication local unless a shared validated command object becomes clearly useful.

## Migration Plan

1. Add `ARCHIVED` to Java `ListingStatus`.
2. Add a Flyway migration for listing status database constraints if the current schema restricts status values.
3. Implement service/controller/repository changes and tests.
4. Run `mvnw.cmd test`.
5. Run `mvnw.cmd verify -Popenapi-export` and review `openapi/roomhub-b2b.openapi.json`.

Rollback strategy: remove the new endpoints and status transition code before archiving the change. If a status migration has been deployed, keep `ARCHIVED` accepted until any archived rows are restored or deleted.

## Open Questions

- None.
