## 1. Preparation and boundaries

- [x] 1.1 Re-read the finalized proposal, design, and spec before implementation.
- [x] 1.2 Inspect the affected frontend files and confirm the work can be completed without backend API or database changes.
- [x] 1.3 If a backend change appears necessary, stop implementation and explain the required backend change before editing anything outside frontend.

## 2. Low-risk UI state changes

- [x] 2.1 Gate the project information banner so it renders only for unauthenticated users.
- [x] 2.2 Keep the catalog filter UI available while a search is active instead of collapsing it immediately after submit.
- [x] 2.3 Show the active catalog filters in the search UI while filtering is in use.
- [x] 2.4 Run lint/build for the banner and catalog search changes.
- [x] 2.5 Manually check the main page as guest and authenticated user, and check that active catalog filters are visible and removable.

## 3. Medium-risk listings refresh

- [x] 3.1 Extract a page-owned polling or refresh hook for the listings page.
- [x] 3.2 Use the polling hook on the listings page and clean it up on unmount so the page refreshes automatically.
- [x] 3.3 Run lint/build for the listings refresh changes.
- [x] 3.4 Manually check that the listings page refreshes without a manual refresh button and stops refreshing after leaving the page.

## 4. Higher-risk booking conflict handling

- [x] 4.1 Reconfirm the backend duplicate-booking behavior from the existing frontend-facing error shape before changing frontend handling.
- [x] 4.2 Extend the booking API error parsing or helper logic so the duplicate application `409` is recognized as a recoverable booking conflict when the backend response identifies it.
- [x] 4.3 Keep the booking form state visible and show a booking-specific duplicate conflict message when that error occurs.
- [x] 4.4 Refresh availability after the conflict so the user can adjust the selection without reloading the page.
- [x] 4.5 Run lint/build for the duplicate conflict flow.
- [x] 4.6 Manually check duplicate booking submission and a normal booking conflict if the local backend state allows it.

## 5. Highest-risk calendar simplification

- [x] 5.1 Define the listing-detail availability view behavior separately from the booking-page selection flow.
- [x] 5.2 Simplify the listing detail calendar so it shows booked, partially booked, and available times without duplicating booking-page slot-selection controls.
- [x] 5.3 Confirm the booking page calendar still behaves as before after the detail calendar update.
- [x] 5.4 Run lint/build for the listing detail and booking calendar routes.
- [x] 5.5 Manually check the listing detail calendar, the booking page calendar, and the visual correctness of text, errors, layout, and availability states.

## 6. Final verification and handoff

- [x] 6.1 Run the full frontend verification available in the project.
- [x] 6.2 Review changed files and confirm the implementation stayed inside frontend scope.
- [x] 6.3 Summarize changed files, completed behavior, validation results, and any manual checks still needed.
