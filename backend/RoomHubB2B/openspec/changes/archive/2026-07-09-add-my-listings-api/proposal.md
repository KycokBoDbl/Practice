## Why

Landlords need a dedicated "My listings" view on the frontend that shows both public and hidden spaces owned by their organization. The public catalog only returns published listings and therefore cannot power owner-side management of hidden listings.

## What Changes

- Add a protected landlord API operation for retrieving listings owned by the authenticated landlord organization.
- Return both `PUBLISHED` and `ARCHIVED` listings so the frontend can render active and hidden items in "My listings".
- Keep ownership derived from the bearer access token; clients must not pass `ownerOrganizationId` or another organization identifier.
- Include each listing's lifecycle status in the management response so the frontend can distinguish published and hidden listings.
- Publish the new operation and response schema in runtime OpenAPI and the exported `openapi/roomhub-b2b.openapi.json`.

Example response:

```json
[
  {
    "id": 42,
    "title": "Meeting room in the city center",
    "city": "Barnaul",
    "pricePerHour": 2500.00,
    "capacity": 20,
    "spaceType": "MEETING_ROOM",
    "imageUrl": "https://example.com/listing-42.jpg",
    "description": "Screen and flip chart",
    "address": "Lenina Avenue, 10",
    "ownerOrganizationName": "Landlord LLC",
    "status": "PUBLISHED"
  }
]
```

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `landlord-listing-management`: add owner-scoped read operation for all non-deleted landlord listings visible in the management UI.
- `openapi-contract-publishing`: document and export the new protected operation and its response schema.

## Impact

- API: new protected `GET` operation under `/api/listings/**` for the authenticated landlord's own listings.
- DTOs: new or extended management response shape that includes listing `status`.
- Persistence: no schema change; the operation filters existing `listings.owner_organization_id` and `status`.
- Security: endpoint requires bearer authentication and role `LANDLORD`; `TENANT` receives `403`, missing or invalid authentication receives `401`.
- Frontend contract: additive change; public `GET /api/listings` remains unchanged.
