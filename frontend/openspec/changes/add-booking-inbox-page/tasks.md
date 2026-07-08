## 1. API Contract

- [x] 1.1 Add `BookingInboxItem` and optional inbox filter types to `src/types/booking.ts`.
- [x] 1.2 Add `getBookingInbox` to `src/api/bookings.ts`, calling `GET /api/bookings` with optional `status`.
- [x] 1.3 Reuse existing booking API error parsing for inbox load failures.

## 2. Inbox Page

- [x] 2.1 Create a protected booking inbox page module under `src/pages/BookingInbox`.
- [x] 2.2 Load inbox data on page open and render loading, empty, error, and loaded states.
- [x] 2.3 Render listing title, status, interval, total price, counterparty organization, created/updated time, and detail link for each item.
- [x] 2.4 Add a retry or refresh control that reloads inbox data from the backend.

## 3. Filtering And Status Awareness

- [x] 3.1 Add status label formatting shared with or consistent with the booking detail page.
- [x] 3.2 Add a simple all/action-required/status grouping control for inbox items.
- [x] 3.3 Make `REQUESTED` prominent for landlords and `AWAITING_CONFIRMATION` prominent for tenants.
- [x] 3.4 Ensure returning to the inbox after detail/transition reloads fresh backend data.

## 4. Routing And Header

- [x] 4.1 Add a protected `/bookings` route while preserving `/bookings/:bookingId`.
- [x] 4.2 Add an authenticated Header navigation entry to the booking inbox.
- [x] 4.3 Ensure guests do not see protected booking inbox navigation and direct access redirects through existing auth guard.
- [x] 4.4 Add or preserve useful back links between inbox, detail, and catalog/booking routes.

## 5. Verification

- [x] 5.1 Run lint/build and fix TypeScript or routing issues.
- [ ] 5.2 Manually verify tenant can open outgoing bookings and navigate to detail.
- [ ] 5.3 Manually verify landlord can open incoming bookings and navigate to detail.
- [ ] 5.4 Manually verify status changes are visible after refresh or returning from detail.
- [ ] 5.5 Confirm existing booking creation, detail, history, and transition flows still work.
