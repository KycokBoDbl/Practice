## 1. Contract And Data Shape

- [x] 1.1 Define `BookingInboxItemDto` with list-safe fields and OpenAPI annotations. Expected files: `src/main/java/ru/esie/practice/roomhubb2b/booking/dto/BookingInboxItemDto.java`.
- [x] 1.2 Decide timestamp source for `createdAt` and `updatedAt`; add minimal Flyway/JPA support only if the current schema cannot provide them. Expected files: `src/main/resources/db/migration/*`, `BookingEntity.java`, `BookingStatusHistoryEntity.java`.
- [x] 1.3 Add repository queries for tenant and landlord inbox visibility with stable recency ordering. Expected files: `BookingRepository.java`.

## 2. Inbox Endpoint

- [x] 2.1 Add a read-only booking inbox service method that derives visible bookings from `BookingActor`. Expected files: `BookingService.java`.
- [x] 2.2 Add authenticated controller endpoint for the inbox, including optional status filter validation. Expected files: `BookingController.java`.
- [x] 2.3 Ensure `TENANT` only receives outgoing bookings and `LANDLORD` only receives incoming bookings for owned listings. Expected files: `BookingService.java`, `BookingRepository.java`.
- [x] 2.4 Preserve existing detail, history, and transition endpoint behavior. Expected files: existing booking service/controller files only if needed.

## 3. Verification

- [x] 3.1 Add tests for tenant inbox visibility, landlord inbox visibility, non-participant isolation, unauthenticated access, and status filtering. Expected files: booking controller/service test files under `src/test/java`.
- [x] 3.2 Run backend tests with Maven and confirm existing booking workflow tests still pass. Expected command: `./mvnw test`.
- [x] 3.3 Run or update OpenAPI export and verify the inbox endpoint and schema are present. Expected files: `openapi/roomhub-b2b.openapi.json`.
