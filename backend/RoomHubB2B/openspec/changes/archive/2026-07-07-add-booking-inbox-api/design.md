## Context

The booking workflow already supports creating a booking, reading one booking by id, reading its status history, and applying role-specific transitions. The frontend can therefore render a booking detail page only when it already knows `bookingId`.

The next frontend step needs a Header entry for authenticated users that opens a list of relevant booking requests. That requires a backend-owned list because local frontend state cannot discover bookings created by another user, another browser session, or another organization participant.

## Goals / Non-Goals

**Goals:**
- Provide a read-only participant booking inbox endpoint for the current organization.
- Let tenants list their outgoing booking requests.
- Let landlords list incoming booking requests for their owned listings.
- Return stable summary data suitable for a frontend table/list and links to booking detail.
- Preserve existing booking detail, history, and transition behavior.
- Keep the MVP additive and avoid introducing notification storage until unread/read semantics are required.

**Non-Goals:**
- Real-time notifications through WebSocket or Server-Sent Events.
- Persistent unread/read notification counters.
- Email, push, or external messaging.
- Cross-organization admin search.
- Changing booking lifecycle transitions or pricing behavior.

## Decisions

### Decision: Add a participant inbox endpoint under bookings

Use an authenticated read endpoint such as `GET /api/bookings` for the current user's booking inbox. The endpoint derives the actor role and organization id from the JWT, matching the existing booking endpoints.

Alternative considered: create `/api/notifications` first. That would imply unread state, event persistence, and notification lifecycle. The current frontend need is a navigable list of bookings, so a booking inbox is the smaller and more direct MVP contract.

### Decision: Derive visibility from existing booking relationships

For `TENANT`, return bookings where `booking.tenantOrganization.id` equals the actor organization id. For `LANDLORD`, return bookings where `booking.listing.ownerOrganization.id` equals the actor organization id.

Alternative considered: expose all bookings with client-side filtering. That would leak data and break the existing authorization model.

### Decision: Use a summary DTO instead of reusing detail DTO unchanged

The inbox response should include the existing booking fields plus list-oriented context such as listing title, tenant organization name, landlord organization name, `createdAt`, and `updatedAt`. Keeping this separate avoids inflating the detail DTO contract without need.

Alternative considered: return only `BookingResponseDto`. That would force the frontend to request every listing or booking detail to render a useful list.

### Decision: Keep ordering backend-defined

The default order should put recently created or recently updated bookings first. The exact implementation may use `createdAt DESC, id DESC` initially if `updatedAt` is not stored yet, but the response contract should include timestamps that allow the frontend to render consistent recency.

Alternative considered: require frontend sorting only. That makes pagination and future filtering unreliable.

## Risks / Trade-offs

- [Risk] The booking entity may not currently store `createdAt` or `updatedAt`. -> Add the minimal timestamp fields through Flyway if missing, or define `updatedAt` from the latest status history entry if that better matches the existing model.
- [Risk] Returning organization names can reveal counterparty data earlier than intended. -> Only return names to the two booking participants who can already read the booking detail.
- [Risk] A future notification bell will still need unread counters. -> Treat this change as the backend foundation for an inbox page, not as the full notification system.
- [Risk] Large booking history can grow. -> Add a limit/page contract if implementation shows list size pressure; keep ordering stable from the start.

## Migration Plan

1. Add the read endpoint, repository queries, summary DTO, and tests.
2. Add timestamp support only if the current schema cannot provide `createdAt`/`updatedAt` reliably.
3. Export and commit the updated OpenAPI contract.
4. Frontend can then add Header navigation and a booking inbox page using the new endpoint.
