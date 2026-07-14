## Why

The backend now exposes a public GigaChat-backed listing search endpoint that can convert a natural-language request into listing filters. The frontend should let users use this capability from the catalog while keeping the existing deterministic catalog filters available and unchanged.

## What Changes

- Add an AI-assisted catalog search entry point that submits a natural-language prompt to the backend and renders the returned listings.
- Keep the existing catalog search, URL filters, polling, listing cards, routes, and map-related behavior intact.
- Show clear AI-search loading, empty, active, reset, and recoverable error states.
- Treat backend `400` responses as prompt/input errors and backend `502` responses as temporary AI-search unavailability.
- Configure the integrated runtime so the backend receives the required `ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY` from local environment variables or `.env` without exposing the secret to the browser bundle.
- Fix user-visible mojibake in frontend catalog/search files touched by this change.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `frontend-catalog-search`: Add AI-assisted search behavior to the catalog while preserving current search/filter semantics.
- `backend-api-contract`: Document frontend consumption of the existing public AI listing search API contract.
- `project-runtime-integration`: Require GigaChat backend runtime configuration for composed local startup.

## Impact

- Frontend catalog code under `src/pages/Spaces`.
- Frontend listing API boundary under `src/api`.
- Shared frontend listing error parsing/types if needed.
- Root runtime configuration may need an environment variable pass-through for `ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY`; implementation must stop for user approval before editing backend or root non-frontend files.
- No backend API behavior changes are in scope.
