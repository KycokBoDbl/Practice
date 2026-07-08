## 1. API Contract And Types

- [x] 1.1 Update listing response types to allow nullable `description` and `imageUrl`.
- [x] 1.2 Add `CreateListingRequest` or equivalent publication request type.
- [x] 1.3 Add `publishListing` to the listing API client using `POST /api/listings`.
- [x] 1.4 Reuse existing ProblemDetail parsing for publication errors.

## 2. Protected Route And Navigation

- [x] 2.1 Add a protected listing publication route.
- [x] 2.2 Add a landlord-only Header navigation entry to publication.
- [x] 2.3 Ensure guests are routed through existing login behavior.
- [x] 2.4 Ensure tenants see a safe role restriction state and cannot submit publication.

## 3. Publication Form Core

- [x] 3.1 Create the publication page and form structure.
- [x] 3.2 Add required fields for title, space type, city, address, capacity, price per hour, description, and image URL.
- [x] 3.3 Add client-side validation for required, numeric, and image URL rules.
- [x] 3.4 Implement submit, pending, success, and recoverable error states.
- [x] 3.5 Navigate or link to the created listing after successful publication.

## 4. User-Friendly Assistants

- [x] 4.1 Add a Russia-only city suggestion source with a broad built-in city list.
- [x] 4.2 Combine built-in cities with cities from loaded catalog data for autocomplete suggestions.
- [x] 4.3 Add selectable compact amenity chips with a reasonably broad base.
- [x] 4.4 Include selected amenity chips in the submitted description in readable form.
- [x] 4.5 Add image placeholder behavior for empty image URL values.
- [x] 4.6 Add localized price formatting and a compact final summary.

## 5. Existing Listing UI Compatibility

- [x] 5.1 Update catalog cards to render a placeholder when `imageUrl` is null or empty.
- [x] 5.2 Update listing detail to render a placeholder when `imageUrl` is null or empty.
- [x] 5.3 Update listing detail to handle nullable description safely.

## 6. Verification

- [x] 6.1 Run lint/build and fix TypeScript or route issues.
- [x] 6.2 Manually verify landlord can publish a listing and open the created detail page.
- [x] 6.3 Manually verify tenant cannot publish.
- [x] 6.4 Manually verify guest publication route returns through authentication.
- [x] 6.5 Manually verify existing catalog, listing detail, booking, and booking inbox flows still work.
