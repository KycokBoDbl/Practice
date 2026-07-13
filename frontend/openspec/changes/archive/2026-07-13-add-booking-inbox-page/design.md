## Context

The frontend already has a booking creation flow, a protected booking detail route, history rendering, and role-aware transition controls. The missing piece is discovery: a participant cannot navigate to all bookings unless they already have a booking detail link.

The backend now provides `GET /api/bookings`, returning booking inbox items for the authenticated user's organization. This supports the immediate product need of a Header entry and a bookings list. The user explicitly dropped the bell notification scope, so this design avoids unread counters and push-style notification UI.

## Goals / Non-Goals

**Goals:**
- Add a protected booking inbox page reachable from Header for authenticated users.
- Reuse the shared booking API boundary and domain typing style.
- Display enough information for tenants and landlords to understand which bookings changed and which need attention.
- Make status changes discoverable through recency ordering, status labels, manual refresh, and returning from detail after actions.
- Preserve existing catalog, booking creation, booking detail, and transition behavior.

**Non-Goals:**
- Bell notification menu, unread counts, read/unread state, WebSocket, SSE, polling, email, or push notifications.
- Backend changes.
- Replacing the booking detail page with the inbox.
- Admin/global booking search.

## Decisions

### Decision: Route the inbox as a protected bookings page

Use a protected route such as `/bookings` for the list and keep `/bookings/:bookingId` for detail. This matches existing route naming and makes the Header link stable.

Alternative considered: put the list under `/profile`. That hides an operational workflow inside profile settings and makes the Header entry less direct.

### Decision: Use the backend inbox as the source of truth

The page should load `GET /api/bookings` through `src/api/bookings.ts` and typed `BookingInboxItem` data. It must not derive the list from local booking creation state.

Alternative considered: store created booking ids in local storage. That would not work for landlords and would fail across sessions/devices.

### Decision: Inform users through an actionable inbox, not notifications

Without backend unread state, the UI should not fake notification counts. Instead, the inbox should make changes visible by ordering recent updates first, showing `updatedAt`, using clear status labels, and offering refresh. The detail page should already refresh state after transitions; returning to the inbox should reload the list.

Alternative considered: frontend-only badges from localStorage. That would be misleading because another browser or participant action would not be reflected reliably.

### Decision: Segment by role-relevant status groups

For `LANDLORD`, `REQUESTED` bookings are most actionable. For `TENANT`, `AWAITING_CONFIRMATION` bookings are most actionable. A compact filter or segmented control can expose "All", "Action required", and terminal statuses while still allowing a full history list.

Alternative considered: only a flat list. A flat list is simpler but makes the main workflow harder once bookings accumulate.

## Risks / Trade-offs

- [Risk] Users expect real-time updates. -> Use clear refresh behavior and recency timestamps; document real notifications as future backend-backed scope.
- [Risk] The Header may become crowded on small screens. -> Keep the authenticated link short and align with existing Header styling.
- [Risk] Backend returns only participant-visible bookings, so empty state may be ambiguous. -> Empty copy should distinguish "no requests yet" from load errors.
- [Risk] Time parsing may be inconsistent with existing detail page. -> Reuse existing date/time formatting helpers or extract a shared local helper during implementation.
