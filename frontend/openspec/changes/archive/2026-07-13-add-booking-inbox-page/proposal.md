## Why

Participants can currently open a booking only when they already know its id, which leaves landlords without a frontend entry point for incoming requests and tenants without a full history of their requests. The backend now exposes a participant booking inbox, so the frontend can add a discoverable booking list without building notification counters.

## What Changes

- Add typed frontend support for `GET /api/bookings` and the `BookingInboxItem` response shape.
- Add an authenticated bookings inbox route for tenants and landlords.
- Add a Header navigation entry for authenticated users that opens the bookings inbox.
- Render a scan-friendly list of booking requests with status, listing title, counterparty, interval, price, and last update time.
- Let users filter or segment bookings by actionable/status groups when useful for the MVP.
- Link every inbox item to the existing booking detail route.
- Surface status changes through list refresh, recency ordering, visible status labels, and after-action refresh from detail rather than a bell notification badge.
- Keep the change additive: existing booking creation, detail, history, and transition routes remain unchanged.

## Capabilities

### New Capabilities
- `frontend-booking-inbox`: Authenticated participant list page for outgoing tenant bookings and incoming landlord bookings.

### Modified Capabilities
- `backend-api-contract`: Add frontend consumption of the backend booking inbox endpoint.
- `frontend-application-contract`: Add authenticated Header navigation and route behavior for the booking inbox.
- `frontend-booking-workflow`: Add booking discovery and status-change awareness through the inbox page.

## Impact

- Frontend API/types: extend booking types and booking API client.
- Router/navigation: add a protected inbox route and authenticated Header link.
- UI: add a booking inbox page with loading, empty, error, filtered, and item states.
- Existing booking detail: may receive a back link to the inbox, but current detail/transition behavior must remain compatible.
- Backend: no new backend change is required for this frontend scope.
