## Why

The frontend needs a reliable backend source for a Header entry that opens a page with the current user's booking requests. The current booking API only supports reading a booking by a known id, so landlords cannot discover incoming requests and tenants cannot open their full request history without external state.

## What Changes

- Add a participant booking inbox API that returns bookings visible to the authenticated user's organization.
- Support both roles:
  - `TENANT` receives bookings created by its organization.
  - `LANDLORD` receives bookings for listings owned by its organization.
- Return enough summary data for a frontend list view: booking id, listing id/title, status, interval, prices, confirmation deadline, participant organization names where safe, and timestamps useful for ordering.
- Add optional filters for status and role-specific direction where they do not weaken authorization.
- Keep existing booking detail, history, and transition endpoints compatible.
- Publish the updated OpenAPI contract after implementation.

Example response shape:

```json
[
  {
    "id": 12,
    "listingId": 4,
    "listingTitle": "Conference room on Lenin Ave",
    "status": "REQUESTED",
    "startAt": "2026-07-10T10:00",
    "endAt": "2026-07-10T13:00",
    "pricePerHour": 2500.00,
    "totalPrice": 7500.00,
    "confirmationDeadline": null,
    "tenantOrganizationName": "Demo Tenant LLC",
    "landlordOrganizationName": "Demo Landlord LLC",
    "createdAt": "2026-07-07T12:40",
    "updatedAt": "2026-07-07T12:40"
  }
]
```

## Capabilities

### New Capabilities
- `booking-inbox`: Read-only participant booking list API for tenants and landlords.

### Modified Capabilities
- `booking-workflow`: Add list/read requirements for discovering participant bookings without changing existing booking lifecycle behavior.
- `openapi-contract-publishing`: The exported OpenAPI contract must include the new inbox endpoint and DTO schema.

## Impact

- Backend API: new authenticated `GET /api/bookings` or equivalent participant inbox endpoint.
- Backend data access: new booking repository queries ordered by backend-defined recency.
- DTOs/OpenAPI: new booking summary DTO and documented query parameters.
- Database: no new table is required for the inbox MVP; existing `bookings`, `listings`, and organization relations are reused.
- Frontend contract: additive change only; existing endpoints and response fields must remain compatible.
