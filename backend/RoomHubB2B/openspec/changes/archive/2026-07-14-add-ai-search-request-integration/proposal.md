## Why

Tenants should be able to search listings in natural language instead of manually mapping their intent to UI filters. This change adds an MVP AI-assisted search path where the backend extracts structured listing filters from a user prompt with GigaChat and returns matching published listings for the frontend to render.

## What Changes

- Add a public `POST /api/listings/ai-search` endpoint that accepts a natural-language prompt and returns several matching published listings.
- Integrate with GigaChat API using OAuth client credentials configured through backend environment properties.
- Convert the GigaChat response into a strict, backend-validated filter object before querying listings.
- Support MVP filter fields aligned with existing listing data: `city`, `spaceType`, `minCapacity`, `maxPricePerHour`, and result `limit`.
- Return existing `ListingResponseDto` items so frontend listing cards can reuse the current response contract.
- Return validation or integration errors as `application/problem+json` without changing `GET /api/listings`.

Example request:

```http
POST /api/listings/ai-search
Content-Type: application/json

{
  "prompt": "Нужен конференц-зал в Москве на 30 человек до 5000 рублей в час"
}
```

Example successful response:

```json
[
  {
    "id": 42,
    "title": "Conference hall near Belorusskaya",
    "city": "Москва",
    "pricePerHour": 4500.00,
    "capacity": 40,
    "spaceType": "CONFERENCE_HALL",
    "imageUrl": "https://example.com/hall.jpg",
    "description": "Hall with projector and reception area",
    "address": "Лесная улица, 5",
    "ownerOrganizationName": "ООО Пространства",
    "latitude": 55.7781,
    "longitude": 37.5864
  }
]
```

## Capabilities

### New Capabilities
- `ai-listing-search`: Natural-language listing search that extracts supported listing filters through GigaChat and returns matching published listings.

### Modified Capabilities
- `openapi-contract-publishing`: Runtime and exported OpenAPI documents must describe the new AI listing search endpoint, request schema, response schema, and documented error responses.

## Impact

- Backend API: new `POST /api/listings/ai-search` endpoint; no breaking changes to existing endpoints.
- Backend integration: new GigaChat OAuth/token client and chat completion client configured by environment variables such as authorization key, OAuth URL, chat URL, model, timeouts, and result limit bounds.
- Listing search: repository/service query logic must support filtering published listings by nullable structured criteria derived from the prompt.
- Frontend contract: frontend can submit a prompt and render the returned `ListingResponseDto[]`; existing `GET /api/listings` response remains compatible.
- Database: no new tables or columns are required for the MVP.
- Operations: production and shared environments must provide the GigaChat authorization key as a secret; the value must not be logged or committed.
