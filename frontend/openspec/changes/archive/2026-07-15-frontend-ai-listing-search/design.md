## Context

The backend branch adds a public `POST /api/listings/ai-search` endpoint. The endpoint accepts a natural-language `prompt`, asks GigaChat to extract a strict listing filter, and returns standard published listing DTOs. The frontend catalog already owns search/filter state, URL query parsing, polling of the ordinary listing catalog, and card rendering.

The backend AI contract is intentionally narrow: it returns listings only. It does not return the parsed filter, an explanation, confidence, or a semantic ranking. The frontend must present it as AI-assisted catalog search without claiming unavailable backend behavior.

## Goals / Non-Goals

**Goals:**

- Add AI-assisted listing search to the catalog using the existing backend endpoint.
- Preserve ordinary catalog search, filters, polling, routes, listing cards, and map behavior.
- Keep GigaChat credentials backend-only and configure the composed runtime through environment variables.
- Make AI-search loading, empty, active, reset, validation, and service-unavailable states explicit.
- Keep frontend implementation inside `frontend` unless the user explicitly approves root/backend runtime edits.

**Non-Goals:**

- No backend API changes.
- No frontend exposure of the raw GigaChat key.
- No streaming, chat history, generated explanations, or parsed-filter display unless the backend later returns that data.
- No replacement of deterministic filters with AI search.
- No semantic text search over title, description, or address beyond what the current backend endpoint supports.
- No redesign of the catalog.

## Decisions

1. Add AI search as a catalog-owned mode.

   The catalog page will keep the normal listings poller as the base dataset. AI search will be a separate submitted state that displays the backend AI result set while active. Clearing AI search returns the user to the current ordinary catalog/filter view.

   Alternative considered: write AI results into the same URL filter model. This would imply the frontend knows the parsed filter, but the backend does not return it.

2. Keep the API boundary small.

   `src/api/listings.ts` should expose an `aiSearchListings({ prompt })` function returning `Listing[]`, plus a parser/kind for `400` and `502` if needed. The response shape should reuse the existing `Listing` type because the backend returns the standard listing DTO.

   Alternative considered: introduce a new frontend DTO type. That adds duplication without a different response shape.

3. Treat prompt text as UI state, not a durable route contract.

   Ordinary deterministic filters already own URL query params. AI search should show an active search chip/summary and reset control, but it should not invent URL parameters unless implementation later proves this is necessary for navigation ergonomics.

   Alternative considered: store AI prompt in URL. This would make refresh/share behavior better but increases interaction complexity with existing filters and can leak user free-form text into browser history.

4. Keep credentials out of frontend artifacts.

   Specs and implementation should require `ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY` to be present for backend runtime, but must not store the raw key in tracked files. The provided key can be used by the developer in local `.env` or shell environment.

   Alternative considered: put the concrete key into specs or compose files. This is rejected because tracked secrets are unsafe and unnecessary for documenting behavior.

5. Preserve current backend constraints in user messaging.

   Empty states and copy should avoid promising full semantic search. The frontend can say that the assistant searches suitable listings from the request, but not that it understands every detail or searches every field.

## Risks / Trade-offs

- Backend returns only listings, not the parsed filter -> The frontend cannot accurately show "AI understood these filters"; show the submitted prompt and result count instead.
- Exact city matching can produce empty results -> Provide a clear empty state and reset path back to ordinary catalog.
- GigaChat outages surface as `502` -> Keep existing catalog usable and show a recoverable message.
- Root Docker Compose currently does not pass the GigaChat key to backend -> During implementation, stop and request approval before editing root runtime files outside `frontend`.
- Existing catalog files contain mojibake -> Fix touched user-visible strings as part of this change and validate with the existing mojibake check where possible.
