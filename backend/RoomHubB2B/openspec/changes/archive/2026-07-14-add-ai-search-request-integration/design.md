## Context

RoomHub currently exposes `GET /api/listings` as a public catalog endpoint that returns all `PUBLISHED` listings. There is no structured listing search contract yet, and there is no AI integration module. The frontend needs a single prompt-based search flow where a user describes desired listing options and receives listing cards back.

GigaChat credentials must remain server-side. The backend will own OAuth token retrieval, prompt construction, response parsing, filter validation, and listing querying. The frontend will only send user text and render the returned `ListingResponseDto[]`.

## Goals / Non-Goals

**Goals:**
- Add a public `POST /api/listings/ai-search` API that accepts a prompt and returns matching published listings.
- Use GigaChat API to extract a constrained listing filter from the prompt.
- Keep the external AI response behind backend validation so only supported filters reach the database query.
- Reuse the existing listing response DTO and published-listing visibility rules.
- Publish the new endpoint in runtime and exported OpenAPI.

**Non-Goals:**
- Do not add conversational state, prompt history, user personalization, or saved searches.
- Do not expose GigaChat tokens, authorization keys, raw prompts, or raw model responses to the frontend.
- Do not implement availability-aware search in this change; date/time availability belongs to the listing availability and booking capabilities.
- Do not change `GET /api/listings` behavior or require authentication for public catalog search.
- Do not add database columns or store AI extraction results for the MVP.

## Decisions

1. Add a dedicated `POST /api/listings/ai-search` endpoint instead of overloading `GET /api/listings`.

   Rationale: a natural-language prompt belongs in a request body, can exceed practical query-string length, and should not be logged as casually as URL parameters. Keeping the endpoint separate avoids breaking the current catalog contract.

   Alternative considered: add a `prompt` query parameter to `GET /api/listings`. Rejected because it mixes deterministic catalog listing with an external AI dependency and makes error behavior harder for frontend to distinguish.

2. Model output will be strict JSON containing only supported filter fields.

   Target extraction shape:

   ```json
   {
     "city": "Москва",
     "spaceType": "CONFERENCE_HALL",
     "minCapacity": 30,
     "maxPricePerHour": 5000.00,
     "limit": 10
   }
   ```

   All fields are nullable except backend-normalized `limit`. `spaceType` must map to the existing enum values: `MEETING_ROOM`, `CONFERENCE_HALL`, `CLASSROOM`, `LOFT`, `SHOWROOM`. The backend rejects malformed JSON, unknown enum values, negative numbers, and limits outside configured bounds.

   Alternative considered: let GigaChat return ready SQL or repository predicates. Rejected because it would create injection and correctness risks and would couple database behavior to model output.

3. The GigaChat integration will be isolated behind service interfaces.

   Proposed package layout:
- `listing/ai/AiListingSearchController`
- `listing/ai/AiListingSearchService`
- `listing/ai/AiListingSearchRequestDto`
- `listing/ai/AiListingSearchFilter`
- `listing/ai/GigaChatProperties`
- `listing/ai/GigaChatTokenClient`
- `listing/ai/GigaChatClient`

   The token client calls the OAuth endpoint using `application/x-www-form-urlencoded`, `scope=GIGACHAT_API_PERS`, `RqUID=<uuid>`, and `Authorization: Basic <configured authorization key>`. The chat client calls a configured chat-completion endpoint with bearer authentication and a system prompt that demands JSON-only output. Access tokens are cached until their reported expiration with a small safety skew.

   Alternative considered: call GigaChat directly from the frontend. Rejected because it exposes backend credentials and makes CORS, rate control, and validation inconsistent.

4. Listing search will use a deterministic repository query after AI extraction.

   The repository query filters only `PUBLISHED` listings and applies nullable criteria:
- `city` case-insensitive exact match after trimming;
- `spaceType` exact enum match;
- `capacity >= minCapacity`;
- `pricePerHour <= maxPricePerHour`;
- `limit` capped by configuration.

   Results should use deterministic ordering such as `pricePerHour ASC, id ASC` so repeated searches are stable.

   Alternative considered: semantic matching against title and description. Rejected for MVP because it needs search indexing or embeddings that are outside the current backend scope.

5. Upstream and validation failures will be explicit.

   Invalid client prompts return `400 Bad Request`. GigaChat network, OAuth, non-success HTTP, empty response, or invalid model JSON failures return `502 Bad Gateway` with `application/problem+json`. Missing required GigaChat configuration fails startup or returns a service-unavailable problem in non-production test wiring, depending on existing configuration patterns.

## Risks / Trade-offs

- [Risk] GigaChat may return prose, invalid JSON, or unsupported values. -> Mitigation: enforce JSON-only prompting, parse with Jackson into a strict DTO, validate and reject invalid model output before querying.
- [Risk] Prompt injection could ask the model to ignore instructions. -> Mitigation: model output is treated as untrusted data, not executable instructions; only whitelisted filter fields are accepted.
- [Risk] External AI latency can slow search. -> Mitigation: configure short connect/read timeouts and return a clear upstream error instead of blocking indefinitely.
- [Risk] Public endpoint can increase GigaChat costs. -> Mitigation: cap prompt length and result limit; future rate limiting can be added separately if abuse appears.
- [Risk] City exact matching may miss synonyms or spelling variants. -> Mitigation: document MVP behavior and keep normalization simple; richer city resolution can be a later search capability.
- [Risk] Secrets can leak through logs. -> Mitigation: never log the authorization key, access token, full raw GigaChat response, or full user prompt at info level.

## Migration Plan

1. Add configuration properties for GigaChat authorization key, OAuth URL, chat URL, model, scope, request timeouts, and max result limit.
2. Add the AI search DTOs, GigaChat clients, service, repository query, and controller endpoint.
3. Add unit tests with mocked GigaChat responses for extraction, token handling, validation, and repository filtering.
4. Add API integration tests for successful search, invalid prompt, empty result, and upstream failure.
5. Update OpenAPI annotations and export `openapi/roomhub-b2b.openapi.json`.
6. Deploy only after the environment has the GigaChat authorization key configured as a secret.

Rollback is low risk because the change adds a new endpoint and does not migrate data. Disable frontend usage or remove the route in a rollback deployment; existing listing APIs continue to work.

## Open Questions

- Confirm the production GigaChat model name and chat-completion URL to use in this project environment.
- Decide whether the frontend wants the extracted filter echoed back in the response later; MVP returns only listings to preserve the existing card-rendering contract.
