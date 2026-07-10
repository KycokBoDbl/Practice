## 1. Database Guard

- [x] 1.1 Add duplicate-detection preflight notes for existing data before adding the constraint. Files: `src/main/resources/db/migration/V12__prevent_duplicate_booking_applications.sql` or implementation notes in the migration. DoD: migration author has a query that identifies duplicate `(tenant_organization_id, listing_id, start_at::date)` groups before the unique index is applied.
- [x] 1.2 Add a Flyway migration creating a PostgreSQL unique expression index named `uk_bookings_tenant_listing_start_date` on `bookings(tenant_organization_id, listing_id, (start_at::date))`. Files: `src/main/resources/db/migration/V12__prevent_duplicate_booking_applications.sql`. DoD: Flyway applies successfully on a clean PostgreSQL database and rejects duplicate rows atomically.

## 2. Booking Creation Logic

- [x] 2.1 Add repository support for checking an existing booking by tenant organization, listing, and `startAt` date range. Files: `src/main/java/ru/esie/practice/roomhubb2b/booking/BookingRepository.java`. DoD: repository can answer whether a booking exists where `startAt >= dayStart` and `startAt < nextDayStart` for the same tenant and listing.
- [x] 2.2 Reject duplicates in `BookingService.create` before saving a new booking. Files: `src/main/java/ru/esie/practice/roomhubb2b/booking/BookingService.java`. DoD: same tenant/listing/start-date duplicate throws `BookingConflictException` with a clear duplicate message before a booking or history row is created.
- [x] 2.3 Translate unique-index races on create into `BookingConflictException` without hiding unrelated integrity failures. Files: `src/main/java/ru/esie/practice/roomhubb2b/booking/BookingService.java`. DoD: concurrent duplicate inserts return the same domain conflict message while other persistence errors still propagate normally.

## 3. API Error Behavior

- [x] 3.1 Verify duplicate booking errors use existing `ProblemDetail` handling. Files: `src/main/java/ru/esie/practice/roomhubb2b/config/ApiExceptionHandler.java`, `src/main/java/ru/esie/practice/roomhubb2b/config/ApiProblemWriter.java`. DoD: duplicate `POST /api/bookings` returns `409 Conflict` with `application/problem+json`; `ApiProblemWriter` remains unchanged unless authentication entrypoint behavior is affected.
- [x] 3.2 Review OpenAPI metadata for `POST /api/bookings`. Files: `src/main/java/ru/esie/practice/roomhubb2b/booking/BookingController.java`, `openapi/roomhub-b2b.openapi.json`. DoD: `409` remains documented for create booking and exported OpenAPI has either no semantic diff or an intentional description-only diff.

## 4. Tests

- [x] 4.1 Add service/integration coverage for duplicate rejection and allowed non-duplicates. Files: `src/test/java/ru/esie/practice/roomhubb2b/booking/BookingWorkflowIntegrationTest.java`. DoD: tests cover same tenant/listing/day conflict, same tenant/listing/different day success, and different tenant/listing/day success.
- [x] 4.2 Add API-level coverage for the duplicate conflict response. Files: `src/test/java/ru/esie/practice/roomhubb2b/booking/BookingApiIntegrationTest.java`. DoD: second duplicate request returns `409`, `application/problem+json`, duplicate detail text, and does not create another booking visible in the inbox.
- [x] 4.3 Add schema or concurrency coverage for the unique guard. Files: `src/test/java/ru/esie/practice/roomhubb2b/booking/BookingSchemaTest.java` or `src/test/java/ru/esie/practice/roomhubb2b/booking/BookingApprovalConcurrencyTest.java`. DoD: database enforcement prevents duplicate rows even when application-level pre-check is bypassed or raced.
- [x] 4.4 Run focused verification. Command: `.\mvnw.cmd test "-Dtest=BookingWorkflowIntegrationTest,BookingApiIntegrationTest,BookingSchemaTest,OpenApiContractTests"`. DoD: focused tests pass, and any unrelated failures are documented separately.
