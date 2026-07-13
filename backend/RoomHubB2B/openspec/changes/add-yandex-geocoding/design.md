## Context

RoomHub already stores `city` and `address` on `ListingEntity` and returns listings through `ListingResponseDto` and `OwnedListingResponseDto`. The frontend now needs geographic coordinates to render each listing address on a Yandex map.

The integration touches persistence, external HTTP calls, listing publication/editing flows, API DTOs, OpenAPI export, and error handling. Yandex Geocoder API accepts forward geocoding requests at `https://geocode-maps.yandex.ru/v1` with `apikey`, `geocode`, `lang`, `format=json`, and returns the selected geo object's `Point.pos` in `longitude latitude` order.

## Goals / Non-Goals

**Goals:**

- Store listing coordinates as first-class database fields.
- Geocode `city + address` before creating a listing.
- Re-geocode only when an edit changes `city` or `address`; keep existing coordinates for edits that change only non-address fields.
- Return `latitude` and `longitude` in public and landlord-owned listing responses.
- Document the additive response fields in runtime OpenAPI and exported OpenAPI JSON.
- Keep the Yandex API key configurable and out of Java source code.

**Non-Goals:**

- Frontend map component implementation.
- Batch backfill for historical listings without coordinates.
- Reverse geocoding, search by map bounds, distance sorting, or geospatial database indexing.
- Changing the create/update request shape to accept client-supplied coordinates.

## Decisions

### Use a dedicated geocoding adapter

Create a small `listing.geocoding` adapter with:

- `GeocodingService` interface returning a value object like `GeoCoordinates(latitude, longitude)`.
- `YandexGeocodingClient` implementation using Spring `RestClient` or the existing Spring Web HTTP stack.
- Configuration properties under `roomhub.geocoding.yandex.*` for API key, base URL, language, result count, and timeout.

Rationale: listing service should depend on geocoding intent, not on Yandex response JSON shape. This keeps tests cheap and makes failure mapping explicit.

Alternative considered: call Yandex directly inside `ListingService`. Rejected because it mixes persistence, authorization, and external JSON parsing in one service.

### Treat coordinates as server-owned data

Add `latitude` and `longitude` to `listings`, `ListingEntity`, `ListingResponseDto`, and `OwnedListingResponseDto`. Do not add coordinate fields to `CreateListingRequestDto` or `UpdateListingRequestDto`.

Rationale: coordinates are derived from address data and should be consistent with the address stored by backend. Accepting client-supplied coordinates would allow stale or conflicting map positions.

Alternative considered: let frontend geocode and send coordinates. Rejected because it exposes external API responsibility to clients and makes stored data less trustworthy.

### Fail the write when geocoding fails

For new listings and address-changing updates, call geocoding before mutating and saving the listing. If Yandex returns no usable result, an HTTP error, invalid JSON, or times out, throw a domain exception mapped to `400 Bad Request` or `502 Bad Gateway` `ProblemDetail`:

- `400 Bad Request` for a syntactically valid request whose address cannot be resolved to coordinates.
- `502 Bad Gateway` for unavailable or invalid external geocoder responses.

Rationale: the requested MVP needs coordinates for frontend display. Silently saving listings without coordinates would produce broken map behavior and make recovery unclear.

Alternative considered: save the listing with `null` coordinates and retry asynchronously. Rejected for this MVP because there is no job infrastructure and the frontend would still receive incomplete map data.

### Parse Yandex coordinate order explicitly

Yandex `Point.pos` is `longitude latitude`. The adapter must parse it into `GeoCoordinates(latitude, longitude)` and validate ranges:

- latitude between `-90` and `90`
- longitude between `-180` and `180`

Rationale: public API field names use the conventional frontend order while the provider response uses the opposite order.

### Make database migration backward-compatible

Add nullable `NUMERIC(9,6)` latitude and `NUMERIC(9,6)` longitude columns. Application-level create/update flows require both values for new/address-updated listings, while legacy rows may remain null until edited or backfilled.

Rationale: nullable columns avoid a deployment-time failure on existing rows. Six decimal places are enough for map marker precision at building level.

Alternative considered: `NOT NULL` with a migration backfill. Rejected because Flyway migrations should not depend on an external geocoder call and demo/prod address quality may vary.

## Risks / Trade-offs

- Yandex latency slows listing create/update -> set a short timeout, keep calls only in address-changing flows, and cover timeout behavior with tests.
- Yandex quota or invalid key blocks publication -> expose clear `ProblemDetail` and keep the API key in environment-backed configuration.
- Ambiguous addresses map to the wrong object -> query with `city, address`, request `results=1`, and preserve the exact address entered by landlord for manual correction through edit.
- Legacy rows can have null coordinates -> response fields are nullable at the contract level until a dedicated backfill is implemented.
- External JSON structure changes -> isolate parsing in the adapter and test it with sample Yandex JSON.

## Migration Plan

1. Add a Flyway migration with `latitude` and `longitude` columns on `listings`.
2. Add entity fields and DTO fields without changing request DTOs.
3. Add Yandex geocoding configuration and client.
4. Inject geocoding into publication and update flows.
5. Extend exception handling for geocoding failures.
6. Update OpenAPI tests and export `openapi/roomhub-b2b.openapi.json`.

Rollback strategy: remove application usage of the columns first, then leave nullable columns in place or drop them in a later migration if the change is abandoned. Because the API change is additive, old frontend consumers can keep ignoring the new fields.

## Open Questions

- Should historical listings be backfilled in a separate operational task before frontend enables map markers for all catalog entries?
- Should the application restrict geocoding to Russia-specific locale/search bounds beyond `lang=ru_RU` for better MVP relevance?
