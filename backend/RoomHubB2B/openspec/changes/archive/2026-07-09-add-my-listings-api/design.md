## Context

Landlord listing management already supports owner-only update, hide, reactivate, and delete operations. The frontend now needs an owner-scoped read operation for the "My listings" tab, including listings hidden from the public catalog.

The public `GET /api/listings` endpoint must remain anonymous and must continue returning only `PUBLISHED` listings. The new operation is management-facing and must derive ownership from the authenticated JWT, not from a request parameter.

## Goals / Non-Goals

**Goals:**

- Provide a protected landlord endpoint that returns the authenticated organization's `PUBLISHED` and `ARCHIVED` listings.
- Include listing `status` in the management response so the frontend can render active and hidden states.
- Preserve existing public listing response behavior and existing mutation authorization rules.
- Publish the new endpoint and response schema in runtime and exported OpenAPI.

**Non-Goals:**

- No database schema changes.
- No pagination, filtering, or sorting controls in this change.
- No tenant-facing listing management view.
- No restoration of deleted listings; physically deleted listings remain absent.

## Decisions

1. Use `GET /api/listings/owned` for the management list.

   Rationale: the path is clearly scoped to listings owned by the authenticated organization while staying under the existing listing resource. It avoids overloading anonymous `GET /api/listings` with conditional behavior based on authentication.

   Alternative considered: `GET /api/listings?scope=owned`. This would mix public and protected semantics on one operation and make OpenAPI/security behavior less explicit.

2. Derive owner organization from `ListingActor.from(jwt)`.

   Rationale: existing publication and management flows already trust the JWT `organizationId` and `role`. Accepting an owner id from query/path would allow enumeration pressure and would duplicate authorization checks already solved in the service layer.

   Alternative considered: `GET /api/organizations/{organizationId}/listings`. This exposes organization ids in the request and creates an extra cross-organization authorization surface.

3. Add a separate management response DTO with `status`.

   Rationale: public `ListingResponseDto` intentionally omits lifecycle status and should stay stable for catalog consumers. A dedicated DTO makes the management contract explicit and allows OpenAPI to document `PUBLISHED`/`ARCHIVED` status without changing public responses.

   Alternative considered: add `status` to `ListingResponseDto`. This is a broader public contract change than needed for "My listings".

4. Query by owner organization and allowed statuses in one repository operation.

   Rationale: the database already stores `owner_organization_id` and `status`, and listing management must not return listings owned by another organization. The query should fetch `ownerOrganization` with the listing so `ownerOrganizationName` can be mapped without lazy-loading surprises.

## Risks / Trade-offs

- [Risk] The initial endpoint returns all owned listings without pagination. -> Mitigation: acceptable for current MVP; add pagination later when listing volumes require it.
- [Risk] A path like `/api/listings/owned` could conflict with a future `GET /api/listings/{listingId}` if path matching is not specific. -> Mitigation: keep the static mapping explicit and cover it with MVC/OpenAPI tests.
- [Risk] Returning hidden listings could expose data if authorization is wrong. -> Mitigation: require `LANDLORD`, derive organization from JWT, and add integration tests for tenant and non-owner isolation.
