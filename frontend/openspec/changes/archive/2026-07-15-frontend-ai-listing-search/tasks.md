## 1. Pre-Implementation Review

- [x] 1.1 Re-read this change's proposal, design, and specs before editing code.
- [x] 1.2 Inspect current catalog/search files and the listing API boundary to confirm the smallest frontend-only implementation path.
- [x] 1.3 Confirm whether integrated manual verification needs root Docker Compose or backend-owned runtime edits; if it does, stop and ask the user before changing non-frontend files.

## 2. API Boundary and Error Handling

- [x] 2.1 Add a frontend request type and API function for `POST /api/listings/ai-search` returning `Listing[]`.
- [x] 2.2 Add AI-search error classification for `400` validation/input errors and `502` temporary service unavailability.
- [x] 2.3 Keep the raw GigaChat authorization key out of frontend code, frontend env variables, and tracked OpenSpec implementation artifacts.

## 3. Catalog Text and Existing Behavior Cleanup

- [x] 3.1 Fix user-visible mojibake strings in catalog/search files touched by the AI-search implementation.
- [x] 3.2 Preserve existing ordinary catalog URL filters, filter badges, reset behavior, and local filtering semantics.
- [x] 3.3 Preserve catalog polling for ordinary listing data and avoid using AI-search results as the polling source.

## 4. AI Search State and UI

- [x] 4.1 Add catalog-owned AI-search prompt state, submitted prompt state, results state, loading state, and recoverable error state.
- [x] 4.2 Add AI-search controls that validate blank and over-1000-character prompts before submission.
- [x] 4.3 Render AI-search active context, result count or prompt context, and a clear/reset control.
- [x] 4.4 Render AI-search loading, empty, validation-error, and unavailable states without clearing ordinary catalog data.
- [x] 4.5 Reuse the existing catalog listing card presentation for AI-search results.

## 5. Runtime Readiness

- [x] 5.1 Document or wire `ROOMHUB_AI_GIGACHAT_AUTHORIZATION_KEY` only as a backend runtime environment value after user approval if non-frontend files must change.
- [x] 5.2 Verify that no `VITE_*` frontend variable or browser-bundled code contains the GigaChat authorization key.

## 6. Verification

- [x] 6.1 Run frontend typecheck/build.
- [x] 6.2 Run frontend lint if available and practical.
- [x] 6.3 Run the mojibake check and fix touched catalog/search text if it reports new issues.
- [ ] 6.4 Manually verify ordinary catalog search/filter behavior still works.
- [ ] 6.5 Manually verify AI-search success, empty-result, invalid-prompt, unavailable-service, and clear/reset flows.
