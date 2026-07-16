## 1. Database and Model

- [x] 1.1 Add Flyway migration for nullable `latitude` and `longitude` columns on `listings` (`src/main/resources/db/migration/V13__add_listing_coordinates.sql`).
- [x] 1.2 Add coordinate fields, getters, constructor/update support, and range-safe value assignment to `ListingEntity` (`src/main/java/ru/esie/practice/roomhubb2b/listing/ListingEntity.java`).
- [x] 1.3 Extend listing response DTOs with nullable `latitude` and `longitude` and update mapping code (`ListingResponseDto.java`, `OwnedListingResponseDto.java`, `ListingService.java`).

## 2. Yandex Geocoding Adapter

- [x] 2.1 Add configuration properties for Yandex geocoding API key, base URL, language, result count, and timeout (`application.properties`, new config/properties class).
- [x] 2.2 Implement `GeoCoordinates`, `GeocodingService`, and `YandexGeocodingClient` under `listing/geocoding`, including parsing `Point.pos` from `longitude latitude` into response fields `latitude` and `longitude`.
- [x] 2.3 Add domain exceptions for unresolvable addresses and external geocoder failures, and map them to `ProblemDetail` responses (`ApiExceptionHandler.java` or related config).
- [x] 2.4 Add unit tests for Yandex response parsing, empty results, invalid coordinate order/ranges, HTTP errors, and timeouts (`src/test/java/.../listing/geocoding`).

## 3. Listing Write Flows

- [x] 3.1 Geocode `city + address` before saving a new listing and persist returned coordinates (`ListingService.java`, listing service tests).
- [x] 3.2 Re-geocode on `PUT /api/listings/{listingId}` only when `city` or `address` changes, and keep coordinates unchanged for non-address edits (`ListingService.java`, listing service tests).
- [x] 3.3 Ensure failed publication geocoding creates no listing and failed edit geocoding leaves the existing listing unchanged (`ListingServiceTest.java`, publication/management integration tests).
- [x] 3.4 Confirm request DTOs do not expose writable `latitude` or `longitude` fields (`CreateListingRequestDto.java`, `UpdateListingRequestDto.java`, DTO/OpenAPI tests).

## 4. API Contract and OpenAPI

- [x] 4.1 Update API integration tests so public listing, publication, edit, and owned-listing responses include `latitude` and `longitude` (`ListingPublicationApiIntegrationTest.java`, management/listing tests).
- [x] 4.2 Update OpenAPI contract tests for coordinate fields on `ListingResponseDto` and `OwnedListingResponseDto` (`OpenApiContractTests.java`).
- [x] 4.3 Export the updated OpenAPI document and review the generated diff (`openapi/roomhub-b2b.openapi.json`).

## 5. Verification

- [x] 5.1 Run focused tests for listing, geocoding, schema, and OpenAPI coverage (`.\mvnw.cmd test` or targeted Maven test selection).
- [x] 5.2 Run OpenSpec validation for `add-yandex-geocoding` and fix any spec or task format issues.
- [x] 5.3 Document the required runtime environment variable for the Yandex API key in the project README or deployment notes if the existing configuration documentation has no suitable place.
