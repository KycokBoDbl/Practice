## 1. Data Model

- [x] 1.1 Extend listing lifecycle with hidden state. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingStatus.java`, `src/main/resources/db/migration/*`. DoD: `ARCHIVED` is accepted by Java and PostgreSQL, existing `PUBLISHED` rows remain valid, and public listing queries still select only `PUBLISHED`.
- [x] 1.2 Add repository methods for owner-aware listing lookup and booking-reference checks. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingRepository.java`, `src/main/java/ru/esie/practice/roomhubb2b/booking/BookingRepository.java`. DoD: service can load a listing by id plus owner organization and can detect whether any booking references a listing.

## 2. Request DTOs and Validation

- [x] 2.1 Add a full listing update request DTO with the same mutable business fields and validation rules as publication. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/dto/UpdateListingRequestDto.java`, existing listing DTO validation tests. DoD: valid update payloads pass, invalid title/price/capacity/spaceType/imageUrl fail with field errors, and the DTO exposes no `ownerOrganizationId`, `status`, or `createdAt`.
- [x] 2.2 Add or update schema/contract tests for the update DTO. Files: `src/test/java/ru/esie/practice/roomhubb2b/listing/*ValidationTest.java`, `src/test/java/ru/esie/practice/roomhubb2b/config/OpenApiContractTests.java`. DoD: tests fail if server-controlled fields become writable.

## 3. Service Behavior

- [x] 3.1 Implement owner-only full edit in `ListingService`. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingService.java`, `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingEntity.java`. DoD: owner landlord updates mutable fields, owner/status/createdAt remain unchanged, tenant gets `403`, non-owned listing gets `404`, and invalid input is rejected before persistence.
- [x] 3.2 Implement hide command in `ListingService`. Files: `ListingService.java`, `ListingEntity.java`. DoD: owner landlord can set status to `ARCHIVED`, repeated hide is idempotent, catalog/availability/booking creation no longer treat the listing as published.
- [x] 3.3 Implement reactivate command in `ListingService`. Files: `ListingService.java`, `ListingEntity.java`. DoD: owner landlord can set an `ARCHIVED` listing back to `PUBLISHED`, repeated activation is idempotent, and catalog/availability/booking creation treat the listing as published again.
- [x] 3.4 Implement delete command with booking protection. Files: `ListingService.java`, `ListingRepository.java`, `BookingRepository.java`. DoD: owner landlord can delete a listing without bookings, listing-only unavailability periods are removed with the listing, and deleting a listing with bookings returns `409` without changing data.

## 4. HTTP API

- [x] 4.1 Add protected listing management endpoints. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingController.java`. DoD: `PUT /api/listings/{listingId}` returns `200` with `ListingResponseDto`, `POST /api/listings/{listingId}/hide` returns `204`, `POST /api/listings/{listingId}/activate` returns `204`, and `DELETE /api/listings/{listingId}` returns `204` when allowed.
- [x] 4.2 Ensure consistent error mapping for management failures. Files: existing listing exception classes and advice. DoD: unauthenticated requests return `401`, tenant role returns `403`, missing/non-owned listing returns `404`, invalid update returns `400`, and delete blocked by bookings returns `409`.

## 5. Tests

- [x] 5.1 Add service tests for edit, hide, reactivate, ownership, and delete conflict behavior. Files: `src/test/java/ru/esie/practice/roomhubb2b/listing/ListingServiceTest.java` or a focused management service test. DoD: tests cover server-controlled fields, owner enforcement, archived status, reactivated status, and booking-reference delete conflict.
- [x] 5.2 Add integration tests for the management API. Files: `src/test/java/ru/esie/practice/roomhubb2b/listing/*IntegrationTest.java`. DoD: tests prove owner success paths, tenant rejection, non-owner rejection, archived listings disappear from `GET /api/listings`, reactivated listings reappear in `GET /api/listings`, and delete behavior matches booking constraints.
- [x] 5.3 Add database/schema tests for status and referential behavior. Files: existing schema/repository tests. DoD: `ARCHIVED` status is persisted, invalid statuses are rejected if DB constraints exist, and deleting listings with booking references is blocked.

## 6. OpenAPI Export

- [x] 6.1 Annotate management endpoints and schemas for OpenAPI. Files: `ListingController.java`, `UpdateListingRequestDto.java`, OpenAPI tests. DoD: runtime `/api/openapi` documents put/hide/activate/delete operations, bearer security, path parameter, request body, success status codes, and error responses.
- [x] 6.2 Run full verification and refresh generated contract. Files: `openapi/roomhub-b2b.openapi.json`. DoD: `mvnw.cmd test` passes, `mvnw.cmd verify -Popenapi-export` succeeds, and exported JSON contains the same management operations, including activate, as runtime OpenAPI with camelCase field names.
