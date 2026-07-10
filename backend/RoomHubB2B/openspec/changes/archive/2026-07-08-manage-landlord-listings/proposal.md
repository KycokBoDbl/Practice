## Why

Landlords can publish listings, but they cannot correct mistakes, temporarily remove a space from the public catalog, or retire an obsolete listing. This blocks the MVP owner workflow because every listing change currently requires manual database intervention or creating duplicate listings.

## What Changes

- Add authenticated landlord operations for managing listings owned by the caller's organization.
- Allow a landlord to edit mutable listing fields: `title`, `description`, `city`, `address`, `pricePerHour`, `capacity`, `spaceType`, and `imageUrl`.
- Allow a landlord to hide a listing from public search by moving it out of the `PUBLISHED` catalog state.
- Allow a landlord to delete its own listing when deletion does not break existing booking history or database referential integrity.
- Keep public `GET /api/listings` limited to published, visible listings.
- Document the new operations in runtime OpenAPI and refresh `openapi/roomhub-b2b.openapi.json`.

Example edit request:

```http
PUT /api/listings/42
Authorization: Bearer <landlord-token>
Content-Type: application/json

{
  "title": "Updated meeting room",
  "description": "Screen, flip chart and coffee area",
  "city": "Barnaul",
  "address": "Lenina Avenue, 10",
  "pricePerHour": 3000.00,
  "capacity": 24,
  "spaceType": "MEETING_ROOM",
  "imageUrl": "https://example.com/listing-42.jpg"
}
```

Example successful response:

```json
{
  "id": 42,
  "title": "Updated meeting room",
  "city": "Barnaul",
  "pricePerHour": 3000.00,
  "capacity": 24,
  "spaceType": "MEETING_ROOM",
  "imageUrl": "https://example.com/listing-42.jpg",
  "description": "Screen, flip chart and coffee area",
  "address": "Lenina Avenue, 10",
  "ownerOrganizationId": 17
}
```

## Capabilities

### New Capabilities
- `landlord-listing-management`: authenticated landlord editing, hiding, deleting, ownership checks, validation, and public catalog visibility rules for owned listings.

### Modified Capabilities
- `openapi-contract-publishing`: publish and export the new landlord listing management operations and request/response schemas.

## Impact

- API: new protected endpoints under `/api/listings/{listingId}` for full edit, hide, and delete.
- Database: `listings.status` must support at least one hidden/non-public value such as `ARCHIVED`; deletion behavior must account for existing `bookings.listing_id` and `listing_unavailability_periods.listing_id` references.
- Backend code: listing controller/service/repository, listing DTOs, validation, exception mapping, OpenAPI tests, publication/management integration tests.
- Frontend contract: generated OpenAPI changes include new operations and any new DTO/status enum values; existing `ListingResponseDto` field names remain camelCase.
