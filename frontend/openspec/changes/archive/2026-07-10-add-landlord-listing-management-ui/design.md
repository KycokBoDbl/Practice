## Context

The frontend already has catalog browsing, listing detail, booking flow, booking detail, booking inbox work in progress, and landlord listing publication. The backend now exposes owner-only listing management operations for update, hide, activate, and delete. The frontend should consume those operations without changing backend behavior.

The central user is an authenticated landlord managing a growing set of spaces. This workflow needs to be operational rather than decorative: a landlord should quickly distinguish active and hidden listings, edit details, remove unavailable spaces from the catalog, restore hidden spaces, and delete safe listings when needed.

## Goals / Non-Goals

**Goals:**
- Add a protected landlord-only "My listings" workflow.
- Provide tabs for active, hidden, and all owned listings.
- Support edit, hide, activate, and permanent delete through the existing backend contract.
- Keep deletion explicit and recover gracefully when backend returns `409`.
- Improve booking inbox visibility with polling and status-focused tabs.
- Rework listing publication into an editable preview-style experience while preserving submission behavior.
- Prepare visual space for a future owner organization field without relying on unavailable backend data.

**Non-Goals:**
- Backend changes.
- Trash bin or temporary deleted-listing recovery.
- Timezone changes.
- Notification bell, unread counters, WebSocket, SSE, or push notifications.
- Showing owner legal name for listings until the backend returns a human-readable owner field.

## Decisions

### Decision: Add a dedicated landlord management route

Use a protected route such as `/my-listings` for owned listing management and expose it from Header only for `LANDLORD` profiles. This keeps management separate from public catalog browsing and from the publication page.

Alternative considered: add management actions directly to catalog cards. That would mix public browsing with owner operations and make role-specific states harder to reason about.

### Decision: Treat backend listing status as the tab source

The management page should group owned listings into active and hidden tabs based on backend-visible listing state. Active listings represent published catalog entries; hidden listings represent archived entries that can be published again.

Alternative considered: infer hidden state locally after actions. That would be fragile across refreshes and sessions.

### Decision: Use full edit form behavior with preview presentation

Editing should reuse the same field semantics as publication: title, type, city, address, capacity, price, description, image URL, and selected amenities embedded in description if that remains the existing storage strategy. The presentation can look like an editable listing preview, but the submitted payload must remain the backend DTO shape.

Alternative considered: create a separate minimal edit modal. That would be faster but would duplicate validation and produce inconsistent publication/editing UX.

### Decision: Keep delete permanent

Deletion should call `DELETE /api/listings/{listingId}` and treat success as permanent removal from the management list. If backend returns `409`, the UI should explain that booking history blocks deletion and suggest hiding instead.

Alternative considered: deleted tab with 12-hour recovery. That requires backend soft-delete state, cleanup scheduling, and restore endpoints, so it is out of scope.

### Decision: Poll booking inbox conservatively

Booking inbox polling should refresh visible booking status from `GET /api/bookings` while the inbox is mounted. It should avoid overlapping requests and should not fake unread state. Manual refresh can remain as an explicit control.

Alternative considered: replace refresh with notification badges. Without backend unread state, badges would be misleading.

## Risks / Trade-offs

- [Risk] Header can become crowded for landlords. -> Keep labels short and consistent: "Создать объявление" and "Мои объявления".
- [Risk] Owner listing API shape may not expose hidden listings if the backend only returns public listings. -> Implement against the generated management contract and verify before UI work; stop before backend changes if a required owner-list endpoint is missing.
- [Risk] Delete is destructive. -> Require explicit confirmation and present hide as the safer alternative when deletion is blocked.
- [Risk] Polling can create unnecessary load. -> Use a modest interval, skip requests while another is pending, and clean up polling on unmount.
- [Risk] Preview-style publication can become visually heavy. -> Keep it operational: compact fields, stable layout, and clear validation, not a marketing-style page.
