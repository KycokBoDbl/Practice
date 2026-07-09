## 1. Contract And API Boundary

- [ ] 1.1 Verify the existing backend contract exposes a way to load owned listings including hidden listings; stop and report if backend support is missing.
- [ ] 1.2 Extend frontend listing domain types for owner management state without breaking existing catalog consumers.
- [ ] 1.3 Add listing management API client functions for owned listing discovery, update, hide, activate, and delete.
- [ ] 1.4 Map listing management API errors into reusable user-facing states, including `400`, `401`, `403`, `404`, and `409`.
- [ ] 1.5 Run lint/build and confirm public catalog and listing detail behavior remain unchanged.

## 2. Route And Header Entry

- [ ] 2.1 Add a protected landlord-only route for owned listing management.
- [ ] 2.2 Add a Header navigation entry labeled "Мои объявления" for authenticated landlords only.
- [ ] 2.3 Ensure guests are routed through existing auth flow and tenants see a safe forbidden or unavailable state.
- [ ] 2.4 Verify existing Header entries, publication route, booking inbox route, and catalog navigation still work.

## 3. My Listings Page

- [ ] 3.1 Create the landlord listing management page structure with loading, loaded, empty, and error states.
- [ ] 3.2 Render owned listing cards or rows with compact operational details and a prepared owner-information area.
- [ ] 3.3 Add tabs for active, hidden, and all listings using backend listing lifecycle state.
- [ ] 3.4 Keep internal owner ids out of user-facing owner labels until a human-readable owner field exists.
- [ ] 3.5 Verify active, hidden, and all tabs preserve layout and do not affect the public catalog UI.

## 4. Edit Listing

- [ ] 4.1 Add an edit flow that opens with the selected listing values populated.
- [ ] 4.2 Reuse publication-compatible validation for title, type, city, address, capacity, price, description, image URL, and helper chip content.
- [ ] 4.3 Submit valid edits through the update endpoint and refresh visible listing data from the backend response.
- [ ] 4.4 Show recoverable messages for validation, forbidden, and not-found edit failures without clearing the management page.
- [ ] 4.5 Run lint/build and manually verify editing does not change listing ownership or lifecycle state.

## 5. Hide, Reactivate, And Delete

- [ ] 5.1 Add hide action for active owned listings and move successful results out of the active tab.
- [ ] 5.2 Add publish-again action for hidden owned listings and move successful results back to the active tab.
- [ ] 5.3 Add permanent delete action with explicit confirmation.
- [ ] 5.4 Handle delete `409 Conflict` by explaining that booking history blocks deletion and keeping the listing visible.
- [ ] 5.5 Handle `401`, `403`, and `404` action failures without exposing unrelated owner data.
- [ ] 5.6 Run lint/build and manually verify hide, activate, successful delete, and blocked delete when possible.

## 6. Publication Preview Refresh

- [ ] 6.1 Rework the publication page layout into an editable listing preview while preserving the current submit payload.
- [ ] 6.2 Keep city suggestions, helper chips, image placeholder, price formatting, and summary behavior working in the preview model.
- [ ] 6.3 Ensure validation scrolls to the first invalid field and price stepping remains aligned to 100-unit increments.
- [ ] 6.4 Add the same future owner-information area without showing internal owner ids.
- [ ] 6.5 Run lint/build and manually verify landlord publication still creates a listing.

## 7. Booking Inbox Status Awareness

- [ ] 7.1 Add conservative polling to the booking inbox while the page is mounted.
- [ ] 7.2 Prevent overlapping inbox requests when polling and manual refresh occur close together.
- [ ] 7.3 Add compact tabs for action-required, confirmed, active, completed, and all bookings.
- [ ] 7.4 Ensure `CONFIRMED`, `IN_PROGRESS`, and `COMPLETED` labels and filters are consistent with booking detail.
- [ ] 7.5 Run lint/build and manually verify tenant and landlord inbox behavior after status changes where possible.

## 8. Final Verification

- [ ] 8.1 Run final lint/build.
- [ ] 8.2 Manually verify landlord can navigate to "Мои объявления", view active/hidden/all tabs, edit, hide, reactivate, and delete safe listings.
- [ ] 8.3 Manually verify tenants and guests cannot access landlord listing management.
- [ ] 8.4 Manually verify existing catalog browsing, listing detail, booking creation, booking detail, booking transitions, and publication still work.
- [ ] 8.5 Review changed files and confirm no backend files were modified.
