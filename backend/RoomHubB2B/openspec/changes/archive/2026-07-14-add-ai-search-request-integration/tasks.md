## 1. Configuration and DTOs

- [x] 1.1 Add `GigaChatProperties` under `src/main/java/ru/esie/practice/roomhubb2b/listing/ai` with authorization key, OAuth URL, chat URL, model, scope, timeout, prompt length, default limit, and max limit settings. DoD: properties bind from `roomhub.ai.gigachat.*`, secrets are not logged, and missing required values fail clearly.
- [x] 1.2 Add AI search request/filter DTOs under `listing/ai` (`AiListingSearchRequestDto`, `AiListingSearchFilter`, extracted-response DTOs). DoD: `prompt` validation rejects null/blank/too-long text and filter validation accepts only supported fields and existing `SpaceType` values.
- [x] 1.3 Document required environment variables in `README.md`. DoD: README tells operators how to provide the GigaChat authorization key and notes that no database migration is required.

## 2. GigaChat Integration

- [x] 2.1 Implement `GigaChatTokenClient` under `listing/ai`. DoD: it sends `scope=GIGACHAT_API_PERS`, `RqUID`, and `Authorization: Basic ...` to the configured OAuth endpoint and caches usable access tokens until expiration with safety skew.
- [x] 2.2 Implement `GigaChatClient` under `listing/ai`. DoD: it sends a JSON-only extraction prompt to the configured GigaChat model endpoint with bearer authentication and returns the raw extracted JSON content to the service.
- [x] 2.3 Add focused unit tests for token and model clients under `src/test/java/ru/esie/practice/roomhubb2b/listing/ai`. DoD: tests cover successful token use, token refresh, OAuth error, timeout/non-success model response, and secret-safe logging expectations where applicable.

## 3. Listing Search Logic

- [x] 3.1 Add repository search support in `ListingRepository.java`. DoD: published listings can be filtered by nullable `city`, `spaceType`, `minCapacity`, and `maxPricePerHour`, ordered deterministically, and capped by the requested limit.
- [x] 3.2 Implement `AiListingSearchService` under `listing/ai`. DoD: it validates prompt input, calls GigaChat, parses the strict filter JSON, normalizes/clamps limit, rejects invalid model output, and maps matches to existing `ListingResponseDto`.
- [x] 3.3 Add service/repository tests for AI search filtering. DoD: tests prove matching behavior, partial filters, empty results, archived listing exclusion, limit enforcement, unknown enum rejection, malformed JSON rejection, and no GigaChat call for invalid prompts.

## 4. REST API and Error Mapping

- [x] 4.1 Add `AiListingSearchController` or extend `ListingController` with `POST /api/listings/ai-search`. DoD: endpoint accepts `AiListingSearchRequestDto`, returns `List<ListingResponseDto>`, is public, and does not alter `GET /api/listings`.
- [x] 4.2 Add exception types and advice mappings for AI search failures. DoD: invalid client prompts return `400 Bad Request`, GigaChat/OAuth/model-output failures return `502 Bad Gateway`, and all errors use `application/problem+json` without credentials or tokens.
- [x] 4.3 Add OpenAPI annotations for the AI search endpoint and schemas. DoD: runtime OpenAPI documents request body, `200`, `400`, and `502` responses, no bearer security, and existing `ListingResponseDto[]` response items.

## 5. Contract, Integration Tests, and Export

- [x] 5.1 Add API integration tests for `POST /api/listings/ai-search`. DoD: tests cover successful prompt search, blank prompt `400`, no matches `200 []`, upstream failure `502`, and archived listings staying hidden.
- [x] 5.2 Update OpenAPI contract tests. DoD: tests fail if `/api/listings/ai-search` is missing from `GET /api/openapi`, if request/response schemas are undocumented, or if bearer security is required.
- [x] 5.3 Export the OpenAPI file. DoD: `openapi/roomhub-b2b.openapi.json` contains the same AI search path and schemas as runtime OpenAPI.
- [x] 5.4 Run focused Maven verification. DoD: listing, AI search, OpenAPI, and contract export tests pass with documented commands.

## 6. Frontend Handoff

- [x] 6.1 Provide the frontend integration contract in README or project API notes. DoD: frontend developers have the endpoint URL, request example, response example, and documented handling for `400`, `502`, and empty array results.
- [x] 6.2 Coordinate frontend implementation outside this backend repo if needed. DoD: the backend contract is ready for a prompt input UI that renders returned `ListingResponseDto[]` as listing cards.
