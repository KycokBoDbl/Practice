## 1. Domain Query and Response Shape

- [x] 1.1 Add an owned-listing management response DTO with all listing card fields plus `status`. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/dto/OwnedListingResponseDto.java`. DoD: DTO exposes `id`, `title`, `city`, `pricePerHour`, `capacity`, `spaceType`, `imageUrl`, `description`, `address`, `ownerOrganizationName`, and `status`.
- [x] 1.2 Add repository support for loading listings by `ownerOrganization.id` and statuses `PUBLISHED`/`ARCHIVED`, fetching `ownerOrganization`. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingRepository.java`. DoD: query cannot return another organization's listings and does not rely on lazy loading for organization name.
- [x] 1.3 Add service method for `LANDLORD` owned listing retrieval. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingService.java`. DoD: landlord receives owned published and archived listings; tenant receives `ListingForbiddenException`; mapper fills `ownerOrganizationName` and `status`.

## 2. API and OpenAPI Contract

- [x] 2.1 Add protected `GET /api/listings/owned` controller operation. Files: `src/main/java/ru/esie/practice/roomhubb2b/listing/ListingController.java`. DoD: operation uses `ListingActor.from(jwt)`, returns `List<OwnedListingResponseDto>`, requires bearer security, and documents `200`, `401`, and `403`.
- [x] 2.2 Update OpenAPI contract tests for the new path and response schema. Files: `src/test/java/ru/esie/practice/roomhubb2b/config/OpenApiContractTests.java`. DoD: tests assert `paths./api/listings/owned.get`, bearer security, array response schema, `status`, and absence of `ownerOrganizationId`.
- [x] 2.3 Export and commit the generated OpenAPI JSON. Files: `openapi/roomhub-b2b.openapi.json`. DoD: exported file contains the same `GET /api/listings/owned` contract as runtime OpenAPI.

## 3. Behavioral Tests

- [x] 3.1 Add integration coverage for owned listing retrieval. Files: `src/test/java/ru/esie/practice/roomhubb2b/listing/ListingPublicationApiIntegrationTest.java`. DoD: response includes owned `PUBLISHED` and `ARCHIVED` listings, excludes another landlord's listings, includes `status`, and omits `ownerOrganizationId`.
- [x] 3.2 Add service unit coverage for role checks and mapping. Files: `src/test/java/ru/esie/practice/roomhubb2b/listing/ListingServiceTest.java`. DoD: landlord path maps status/name correctly; tenant path fails before repository lookup.
- [x] 3.3 Run focused verification. Command: `.\mvnw.cmd test "-Dtest=ListingServiceTest,ListingPublicationApiIntegrationTest,OpenApiContractTests"`. DoD: focused tests pass; any unrelated full-suite failures are documented separately.
